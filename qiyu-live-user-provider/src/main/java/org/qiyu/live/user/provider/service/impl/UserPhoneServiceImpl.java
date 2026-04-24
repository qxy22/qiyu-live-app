package org.qiyu.live.user.provider.service.impl;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.qiyu.live.id.generate.interfac.IdBuilderRpc;
import org.qiyu.live.msg.dto.MsgCheckDTO;
import org.qiyu.live.msg.interfac.ISmsRpc;
import org.qiyu.live.user.DTO.IUserPhoneRpcDTO;
import org.qiyu.live.user.DTO.UserDTO;
import org.qiyu.live.user.provider.cache.JwtUtil;
import org.qiyu.live.user.provider.cache.UserProviderCacheKeyBuilder;
import org.qiyu.live.user.provider.dao.mapper.IUserPhoneMapper;
import org.qiyu.live.user.provider.dao.po.UserPhonePO;
import org.qiyu.live.user.provider.service.IUserPhoneService;
import org.qiyu.live.user.provider.service.IUserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class UserPhoneServiceImpl implements IUserPhoneService {

    private static final int USER_ID_BIZ_CODE = 1;
    private static final long LOGIN_CACHE_EXPIRE_DAYS = 30L;

    @Resource
    private IUserPhoneMapper userPhoneMapper;

    @DubboReference
    private ISmsRpc smsRpc;

    @DubboReference(check = false)
    private IdBuilderRpc idBuilderRpc;

    @Resource
    private IUserService userService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserProviderCacheKeyBuilder cacheKeyBuilder;

    @Override
    public IUserPhoneRpcDTO login(String phone, Integer code) {
        if (!isValidPhone(phone) || code == null) {
            return new IUserPhoneRpcDTO(false, null, null, "phone or code is invalid");
        }

        MsgCheckDTO msgCheckDTO = smsRpc.checkLoginCode(phone, code);
        if (msgCheckDTO == null || !msgCheckDTO.isCheckStatus()) {
            String desc = msgCheckDTO == null ? "sms code check failed" : msgCheckDTO.getDesc();
            return new IUserPhoneRpcDTO(false, null, null, desc);
        }

        UserPhonePO userPhonePO = queryUserPhoneByPhone(phone);
        if (userPhonePO == null || userPhonePO.getUserId() == null) {
            return new IUserPhoneRpcDTO(false, null, null, "phone is not registered");
        }

        Long userId = userPhonePO.getUserId();
        String token = jwtUtil.generateToken(userId);
        refreshLoginCache(userPhonePO, token);
        return new IUserPhoneRpcDTO(true, userId, token, "login success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IUserPhoneRpcDTO register(String phone, Integer code) {
        if (!isValidPhone(phone) || code == null) {
            return new IUserPhoneRpcDTO(false, null, null, "phone or code is invalid");
        }

        Long existUserId = getUserIdByPhone(phone);
        if (existUserId != null) {
            return new IUserPhoneRpcDTO(false, existUserId, null, "phone already registered");
        }

        MsgCheckDTO msgCheckDTO = smsRpc.checkLoginCode(phone, code);
        if (msgCheckDTO == null || !msgCheckDTO.isCheckStatus()) {
            String desc = msgCheckDTO == null ? "sms code check failed" : msgCheckDTO.getDesc();
            return new IUserPhoneRpcDTO(false, null, null, desc);
        }

        Long userId = buildUserId();
        if (userId == null) {
            return new IUserPhoneRpcDTO(false, null, null, "build user id failed");
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setPhone(phone);
        userDTO.setNickName("user" + phone.substring(phone.length() - 4));

        boolean createSuccess = userService.createUser(userDTO);
        if (!createSuccess) {
            return new IUserPhoneRpcDTO(false, null, null, "register failed");
        }

        String token = jwtUtil.generateToken(userId);
        UserPhonePO userPhonePO = new UserPhonePO();
        userPhonePO.setUserId(userId);
        userPhonePO.setPhone(phone);
        userPhonePO.setStatus(1);
        refreshLoginCache(userPhonePO, token);
        return new IUserPhoneRpcDTO(true, userId, token, "register success");
    }

    @Override
    public Long getUserIdByPhone(String phone) {
        UserPhonePO userPhonePO = queryUserPhoneByPhone(phone);
        return userPhonePO == null ? null : userPhonePO.getUserId();
    }

    @Override
    public Long getUserIdByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        Object cacheObj = redisTemplate.opsForValue().get(cacheKeyBuilder.buildUserLoginTokenIndexKey(token));
        Long cachedUserId = castToLong(cacheObj);
        if (cachedUserId != null) {
            Object currentToken = redisTemplate.opsForValue().get(cacheKeyBuilder.buildUserLoginTokenKey(cachedUserId));
            if (currentToken != null && token.equals(String.valueOf(currentToken))) {
                return cachedUserId;
            }
            redisTemplate.delete(cacheKeyBuilder.buildUserLoginTokenIndexKey(token));
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return null;
            }

            Object currentToken = redisTemplate.opsForValue().get(cacheKeyBuilder.buildUserLoginTokenKey(userId));
            if (currentToken != null && token.equals(String.valueOf(currentToken))) {
                redisTemplate.opsForValue().set(
                        cacheKeyBuilder.buildUserLoginTokenIndexKey(token),
                        userId,
                        LOGIN_CACHE_EXPIRE_DAYS,
                        TimeUnit.DAYS
                );
                return userId;
            }
            return currentToken == null ? userId : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean bindPhone(Long userId, String phone) {
        if (userId == null || !isValidPhone(phone)) {
            return false;
        }

        if (getUserIdByPhone(phone) != null) {
            return false;
        }

        UserPhonePO userPhonePO = new UserPhonePO();
        userPhonePO.setUserId(userId);
        userPhonePO.setPhone(phone);
        userPhonePO.setStatus(1);
        boolean bindSuccess = userPhoneMapper.insert(userPhonePO) > 0;
        if (bindSuccess) {
            cacheUserPhone(userPhonePO);
        }
        return bindSuccess;
    }

    private Long buildUserId() {
        try {
            return idBuilderRpc.getSeqId(USER_ID_BIZ_CODE);
        } catch (Exception ignored) {
            return System.currentTimeMillis();
        }
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private UserPhonePO queryUserPhoneByPhone(String phone) {
        if (!isValidPhone(phone)) {
            return null;
        }

        UserPhonePO cachedUserPhone = getCachedUserPhoneObj(phone);
        if (cachedUserPhone != null) {
            return cachedUserPhone;
        }

        UserPhonePO userPhonePO = userPhoneMapper.selectByPhone(phone);
        if (userPhonePO != null) {
            cacheUserPhone(userPhonePO);
        }
        return userPhonePO;
    }

    private void refreshLoginCache(UserPhonePO userPhonePO, String token) {
        if (userPhonePO == null || userPhonePO.getUserId() == null) {
            return;
        }

        Long userId = userPhonePO.getUserId();
        String loginTokenKey = cacheKeyBuilder.buildUserLoginTokenKey(userId);
        Object oldToken = redisTemplate.opsForValue().get(loginTokenKey);
        if (oldToken != null) {
            redisTemplate.delete(cacheKeyBuilder.buildUserLoginTokenIndexKey(String.valueOf(oldToken)));
        }

        redisTemplate.opsForValue().set(
                loginTokenKey,
                token,
                LOGIN_CACHE_EXPIRE_DAYS,
                TimeUnit.DAYS
        );
        redisTemplate.opsForValue().set(
                cacheKeyBuilder.buildUserLoginTokenIndexKey(token),
                userId,
                LOGIN_CACHE_EXPIRE_DAYS,
                TimeUnit.DAYS
        );

        cacheUserPhone(userPhonePO);
    }

    private UserPhonePO getCachedUserPhoneObj(String phone) {
        Object cacheObj = redisTemplate.opsForValue().get(cacheKeyBuilder.buildUserPhoneObjKey(phone));
        if (cacheObj instanceof UserPhonePO userPhonePO) {
            return userPhonePO;
        }
        return null;
    }

    private void cacheUserPhone(UserPhonePO userPhonePO) {
        redisTemplate.opsForValue().set(
                cacheKeyBuilder.buildUserPhoneObjKey(userPhonePO.getPhone()),
                userPhonePO,
                LOGIN_CACHE_EXPIRE_DAYS,
                TimeUnit.DAYS
        );

        redisTemplate.opsForValue().set(
                cacheKeyBuilder.buildUserPhoneListKey(userPhonePO.getUserId()),
                Collections.singletonList(userPhonePO),
                LOGIN_CACHE_EXPIRE_DAYS,
                TimeUnit.DAYS
        );
    }

    private Long castToLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Long.parseLong(stringValue);
        }
        return null;
    }
}
