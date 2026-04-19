package org.qiyu.live.user.provider.utils;

public class TagInfoUtils {

    private TagInfoUtils() {
    }

    public static boolean isContain(Long tagInfo, long tag) {
        if (tagInfo == null || tag <= 0) {
            return false;
        }
        return (tagInfo & tag) == tag;
    }
}
