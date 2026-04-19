package org.qiyu.live.user.interfac;

import org.qiyu.live.user.constants.UserTagsEnum;

public interface IUserTagRpc {

    boolean setTag(Long userId, UserTagsEnum userTagsEnum);

    boolean cancelTag(Long userId, UserTagsEnum userTagsEnum);

    boolean containTag(Long userId, UserTagsEnum userTagsEnum);
}
