package org.qiyu.live.msg.provider.service.impl;

import jakarta.annotation.Resource;
import org.qiyu.live.msg.dto.MsgCheckDTO;
import org.qiyu.live.msg.enums.MsgSendResultEnum;
import org.qiyu.live.msg.provider.config.MsgProviderCacheKeyBuilder;
import org.qiyu.live.msg.provider.config.ThreadPoolManager;
import org.qiyu.live.msg.provider.dao.mapper.SmsMapper;
import org.qiyu.live.msg.provider.dao.po.SmsPO;
import org.qiyu.live.msg.provider.service.ISmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements ISmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);
    private static final int CODE_MIN = 100000;
    private static final int CODE_MAX_EXCLUSIVE = 1000000;
    private static final long LOGIN_CODE_TTL_SECONDS = 60L;

    @Resource
    private SmsMapper smsMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MsgProviderCacheKeyBuilder cacheKeyBuilder;

    @Override
    public MsgSendResultEnum sendMessage(String phone) {
        if (!isValidPhone(phone)) {
            return MsgSendResultEnum.MSG_PARAM_ERROR;
        }
        String key = cacheKeyBuilder.buildSmsLoginCodeKey(phone);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            log.warn("send sms too frequently, phone={}", phone);
            return MsgSendResultEnum.SEND_FAIL;
        }

        int code = ThreadLocalRandom.current().nextInt(CODE_MIN, CODE_MAX_EXCLUSIVE);
        redisTemplate.opsForValue().set(key, code, LOGIN_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        ThreadPoolManager.COMMON_ASYNC_POOL.execute(() -> {
            mockSendSms(phone, code);
            insertOne(phone, code);
        });
        return MsgSendResultEnum.SEND_SUCCESS;
    }

    @Override
    public MsgCheckDTO checkLoginCode(String phone, Integer code) {
        if (!isValidPhone(phone) || code == null) {
            return new MsgCheckDTO(false, "参数错误");
        }
        String key = cacheKeyBuilder.buildSmsLoginCodeKey(phone);
        Object recordCode = redisTemplate.opsForValue().get(key);
        if (recordCode == null) {
            return new MsgCheckDTO(false, "验证码已失效");
        }
        if (String.valueOf(recordCode).equals(String.valueOf(code))) {
            redisTemplate.delete(key);
            return new MsgCheckDTO(true, "成功");
        }
        return new MsgCheckDTO(false, "验证码校验失败");
    }

    @Override
    public void insertOne(String phone, Integer code) {
        SmsPO smsPO = new SmsPO();
        smsPO.setPhone(phone);
        smsPO.setCode(code);
        smsMapper.insert(smsPO);
    }

    private void mockSendSms(String phone, Integer code) {
        log.info("mock sms sending, phone={}, code={}", phone, code);
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
