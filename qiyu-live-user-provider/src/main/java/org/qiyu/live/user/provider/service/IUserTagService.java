package org.qiyu.live.user.provider.service;

import org.qiyu.live.user.constants.UserTagsEnum;

public interface IUserTagService {

    boolean setTag(Long userId, UserTagsEnum userTagsEnum);

    boolean cancelTag(Long userId, UserTagsEnum userTagsEnum);

    boolean containTag(Long userId, UserTagsEnum userTagsEnum);
}
