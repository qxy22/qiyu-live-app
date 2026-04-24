package org.qiyu.live.api.controller;

import org.apache.dubbo.config.annotation.DubboReference;
import org.qiyu.live.msg.enums.MsgSendResultEnum;
import org.qiyu.live.msg.interfac.ISmsRpc;
import org.qiyu.live.user.DTO.IUserPhoneRpcDTO;
import org.qiyu.live.user.interfac.IUserPhoneRpc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/userPhone")
public class UserPhoneController {

    @DubboReference(check = false)
    private ISmsRpc smsRpc;

    @DubboReference(check = false)
    private IUserPhoneRpc userPhoneRpc;

    @GetMapping("/sendLoginCode")
    public MsgSendResultEnum sendLoginCode(String phone) {
        if (!isValidPhone(phone)) {
            return MsgSendResultEnum.MSG_PARAM_ERROR;
        }
        return smsRpc.sendMessage(phone);
    }

    @GetMapping("/login")
    public IUserPhoneRpcDTO login(String phone, Integer code) {
        return userPhoneRpc.login(phone, code);
    }

    @GetMapping("/register")
    public IUserPhoneRpcDTO register(String phone, Integer code) {
        return userPhoneRpc.register(phone, code);
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
