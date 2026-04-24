package org.qiyu.live.user.provider.mq;

import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class CacheDeleteProducer {

    private static final Logger log = LoggerFactory.getLogger(CacheDeleteProducer.class);
    private static final String USER_TAG_CACHE_MESSAGE_PREFIX = "USER_TAG:";

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${qiyu.rocketmq.cache-delete.topic:cache-delete-topic}")
    private String topic;

    public void sendDelayDeleteMessage(Long userId) {
        sendDelayDeleteMessage(String.valueOf(userId));
        log.info("[RocketMQ] send delay delete user cache message, userId={}", userId);
    }

    public void sendDelayDeleteUserTagMessage(Long userId) {
        sendDelayDeleteMessage(USER_TAG_CACHE_MESSAGE_PREFIX + userId);
        log.info("[RocketMQ] send delay delete user tag cache message, userId={}", userId);
    }

    private void sendDelayDeleteMessage(String payload) {
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader(MessageConst.PROPERTY_DELAY_TIME_LEVEL, 1)
                .build();
        rocketMQTemplate.syncSend(topic, message, 3000);
    }
}
