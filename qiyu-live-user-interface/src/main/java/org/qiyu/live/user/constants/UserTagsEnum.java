package org.qiyu.live.user.constants;

import java.io.Serializable;

public enum UserTagsEnum implements Serializable {

    IS_VIP(UserTagFieldNameConstants.TAG_INFO_01, 1L, "VIP user"),
    IS_OLD_USER(UserTagFieldNameConstants.TAG_INFO_01, 1L << 1, "old user"),
    IS_ANCHOR(UserTagFieldNameConstants.TAG_INFO_01, 1L << 2, "anchor user"),
    IS_PAY_USER(UserTagFieldNameConstants.TAG_INFO_01, 1L << 3, "paid user"),
    IS_ACTIVE_USER(UserTagFieldNameConstants.TAG_INFO_01, 1L << 4, "active user"),
    IS_HIGH_VALUE_USER(UserTagFieldNameConstants.TAG_INFO_02, 1L, "high value user"),
    IS_RISK_USER(UserTagFieldNameConstants.TAG_INFO_02, 1L << 1, "risk user"),
    IS_TEST_USER(UserTagFieldNameConstants.TAG_INFO_03, 1L, "test user");

    private final String fieldName;
    private final long tag;
    private final String desc;

    UserTagsEnum(String fieldName, long tag, String desc) {
        this.fieldName = fieldName;
        this.tag = tag;
        this.desc = desc;
    }

    public String getFieldName() {
        return fieldName;
    }

    public long getTag() {
        return tag;
    }

    public String getDesc() {
        return desc;
    }
}
