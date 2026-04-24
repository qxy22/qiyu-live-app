package org.qiyu.live.user.provider.mq;

import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.qiyu.live.user.provider.cache.UserProviderCacheKeyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "${qiyu.rocketmq.cache-delete.topic:cache-delete-topic}",
        consumerGroup = "${qiyu.rocketmq.cache-delete.consumer-group:cache-delete-consumer-group}",
        instanceName = "cache-delete-consumer-instance"
)
public class CacheDeleteConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(CacheDeleteConsumer.class);
    private static final String USER_CACHE_KEY_PREFIX = "qiyu:user:";
    private static final String USER_TAG_CACHE_MESSAGE_PREFIX = "USER_TAG:";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserProviderCacheKeyBuilder cacheKeyBuilder;

    @Override
    public void onMessage(String message) {
        if (message == null || message.isBlank()) {
            log.warn("[RocketMQ] receive empty cache delete message");
            return;
        }

        if (message.startsWith(USER_TAG_CACHE_MESSAGE_PREFIX)) {
            deleteUserTagCache(message);
            return;
        }

        deleteUserCache(message);
    }

    private void deleteUserCache(String message) {
        Long userId = parseUserId(message);
        if (userId == null) {
            return;
        }

        redisTemplate.delete(USER_CACHE_KEY_PREFIX + userId);
        log.info("[RocketMQ] delay double delete user cache completed, userId={}", userId);
    }

    private void deleteUserTagCache(String message) {
        String userIdText = message.substring(USER_TAG_CACHE_MESSAGE_PREFIX.length());
        Long userId = parseUserId(userIdText);
        if (userId == null) {
            return;
        }

        redisTemplate.delete(cacheKeyBuilder.buildUserTagKey(userId));
        log.info("[RocketMQ] delay double delete user tag cache completed, userId={}", userId);
    }

    private Long parseUserId(String userIdText) {
        try {
            return Long.parseLong(userIdText);
        } catch (NumberFormatException e) {
            log.warn("[RocketMQ] invalid cache delete message, message={}", userIdText);
            return null;
        }
    }
}
