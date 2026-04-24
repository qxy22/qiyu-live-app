package org.qiyu.live.user.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.qiyu.live.user.DTO.IUserPhoneRpcDTO;
import org.qiyu.live.user.interfac.IUserPhoneRpc;
import org.qiyu.live.user.provider.service.IUserPhoneService;

@DubboService
public class UserPhoneRpcImpl implements IUserPhoneRpc {

    @Resource
    private IUserPhoneService userPhoneService;

    @Override
    public IUserPhoneRpcDTO login(String phone, Integer code) {
        return userPhoneService.login(phone, code);
    }

    @Override
    public IUserPhoneRpcDTO register(String phone, Integer code) {
        return userPhoneService.register(phone, code);
    }

    @Override
    public Long getUserIdByPhone(String phone) {
        return userPhoneService.getUserIdByPhone(phone);
    }

    @Override
    public Long getUserIdByToken(String token) {
        return userPhoneService.getUserIdByToken(token);
    }

    @Override
    public boolean bindPhone(Long userId, String phone) {
        return userPhoneService.bindPhone(userId, phone);
    }
}
