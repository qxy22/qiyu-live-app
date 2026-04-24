package org.qiyu.live.user.provider.service;

import org.qiyu.live.user.DTO.IUserPhoneRpcDTO;

public interface IUserPhoneService {

    IUserPhoneRpcDTO login(String phone, Integer code);

    IUserPhoneRpcDTO register(String phone, Integer code);

    Long getUserIdByPhone(String phone);

    Long getUserIdByToken(String token);

    boolean bindPhone(Long userId, String phone);
}
