package org.qiyu.live.user.provider.cache;

import org.springframework.stereotype.Component;

@Component
public class UserProviderCacheKeyBuilder {

    private static final String USER_TAG_LOCK_KEY_PREFIX = "qiyu:user:tag:lock:";
    private static final String USER_TAG_KEY_PREFIX = "qiyu:user:tag:";

    public String buildTagLockKey(Long userId) {
        return USER_TAG_LOCK_KEY_PREFIX + userId;
    }

    public String buildUserTagKey(Long userId) {
        return USER_TAG_KEY_PREFIX + userId;
    }
}
