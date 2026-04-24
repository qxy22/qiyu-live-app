package org.qiyu.live.user.provider.cache;

import org.springframework.stereotype.Component;

@Component
public class UserProviderCacheKeyBuilder {

    private static final String USER_TAG_LOCK_KEY_PREFIX = "qiyu:user:tag:lock:";
    private static final String USER_TAG_KEY_PREFIX = "qiyu:user:tag:";
    private static final String USER_LOGIN_TOKEN_KEY_PREFIX = "userLoginToken:";
    private static final String USER_LOGIN_TOKEN_INDEX_KEY_PREFIX = "userLoginToken:index:";
    private static final String USER_PHONE_LIST_KEY_PREFIX = "userPhoneList:";
    private static final String USER_PHONE_OBJ_KEY_PREFIX = "userPhoneObj:";

    public String buildTagLockKey(Long userId) {
        return USER_TAG_LOCK_KEY_PREFIX + userId;
    }

    public String buildUserTagKey(Long userId) {
        return USER_TAG_KEY_PREFIX + userId;
    }

    public String buildUserLoginTokenKey(Long userId) {
        return USER_LOGIN_TOKEN_KEY_PREFIX + userId;
    }

    public String buildUserLoginTokenIndexKey(String token) {
        return USER_LOGIN_TOKEN_INDEX_KEY_PREFIX + token;
    }

    public String buildUserPhoneListKey(Long userId) {
        return USER_PHONE_LIST_KEY_PREFIX + userId;
    }

    public String buildUserPhoneObjKey(String phone) {
        return USER_PHONE_OBJ_KEY_PREFIX + phone;
    }
}
