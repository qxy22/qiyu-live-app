package org.qiyu.live.msg.provider.config;

import org.springframework.stereotype.Component;

@Component
public class MsgProviderCacheKeyBuilder {

    private static final String SMS_LOGIN_CODE_KEY_PREFIX = "qiyu:msg:sms:login:";

    public String buildSmsLoginCodeKey(String phone) {
        return SMS_LOGIN_CODE_KEY_PREFIX + phone;
    }
}
