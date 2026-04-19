package org.qiyu.live.user.provider.service.impl;

import jakarta.annotation.Resource;
import org.qiyu.live.user.constants.UserTagFieldNameConstants;
import org.qiyu.live.user.constants.UserTagsEnum;
import org.qiyu.live.user.provider.cache.UserProviderCacheKeyBuilder;
import org.qiyu.live.user.provider.dao.mapper.IUserTagMapper;
import org.qiyu.live.user.provider.dao.po.UserTagPO;
import org.qiyu.live.user.provider.mq.CacheDeleteProducer;
import org.qiyu.live.user.provider.service.IUserTagService;
import org.qiyu.live.user.provider.utils.TagInfoUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserTagServiceImpl implements IUserTagService {

    private static final long USER_TAG_CACHE_EXPIRE_TIME = 30L;

    @Resource
    private IUserTagMapper userTagMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserProviderCacheKeyBuilder cacheKeyBuilder;

    @Resource
    private CacheDeleteProducer cacheDeleteProducer;

    @Override
    public boolean setTag(Long userId, UserTagsEnum userTagsEnum) {
        if (!isValidRequest(userId, userTagsEnum)) {
            return false;
        }
        evictUserTagCache(userId);
        boolean updateSuccess = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (updateSuccess) {
            cacheDeleteProducer.sendDelayDeleteUserTagMessage(userId);
            return true;
        }

        UserTagPO userTagPO = userTagMapper.selectById(userId);
        if (userTagPO != null) {
            return false;
        }

        if (!tryLockUserTagInit(userId)) {
            return false;
        }
        userTagMapper.initUserTag(userId);
        updateSuccess = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (updateSuccess) {
            cacheDeleteProducer.sendDelayDeleteUserTagMessage(userId);
        }
        return updateSuccess;
    }

    @Override
    public boolean cancelTag(Long userId, UserTagsEnum userTagsEnum) {
        if (!isValidRequest(userId, userTagsEnum)) {
            return false;
        }
        evictUserTagCache(userId);
        boolean updateSuccess = userTagMapper.cancelTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (updateSuccess) {
            cacheDeleteProducer.sendDelayDeleteUserTagMessage(userId);
        }
        return updateSuccess;
    }

    @Override
    public boolean containTag(Long userId, UserTagsEnum userTagsEnum) {
        if (!isValidRequest(userId, userTagsEnum)) {
            return false;
        }
        UserTagPO userTagPO = queryUserTagWithCache(userId);
        if (userTagPO == null) {
            return false;
        }

        String queryFieldName = userTagsEnum.getFieldName();
        if (UserTagFieldNameConstants.TAG_INFO_01.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagPO.getTagInfo01(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAG_INFO_02.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagPO.getTagInfo02(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAG_INFO_03.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagPO.getTagInfo03(), userTagsEnum.getTag());
        }
        return false;
    }

    private boolean isValidRequest(Long userId, UserTagsEnum userTagsEnum) {
        return userId != null && userId > 0 && userTagsEnum != null;
    }

    private UserTagPO queryUserTagWithCache(Long userId) {
        String cacheKey = cacheKeyBuilder.buildUserTagKey(userId);
        Object cachedUserTag = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUserTag instanceof UserTagPO userTagPO) {
            return userTagPO;
        }

        UserTagPO userTagPO = userTagMapper.selectById(userId);
        if (userTagPO != null) {
            redisTemplate.opsForValue().set(cacheKey, userTagPO, USER_TAG_CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        return userTagPO;
    }

    private void evictUserTagCache(Long userId) {
        redisTemplate.delete(cacheKeyBuilder.buildUserTagKey(userId));
    }

    private boolean tryLockUserTagInit(Long userId) {
        String lockKey = cacheKeyBuilder.buildTagLockKey(userId);
        Boolean lockSuccess = redisTemplate.opsForValue().setIfAbsent(lockKey, -1, 3, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(lockSuccess);
    }
}
