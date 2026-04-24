package org.qiyu.live.api.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.qiyu.live.api.annotation.LoginRequired;
import org.qiyu.live.api.context.LoginUserContext;
import org.qiyu.live.api.vo.UserLoginVO;
import org.qiyu.live.api.vo.WebResponseVO;
import org.qiyu.live.msg.enums.MsgSendResultEnum;
import org.qiyu.live.msg.interfac.ISmsRpc;
import org.qiyu.live.user.DTO.IUserPhoneRpcDTO;
import org.qiyu.live.user.interfac.IUserPhoneRpc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/userLogin")
public class UserLoginController {

    private static final int TOKEN_COOKIE_MAX_AGE = 30 * 24 * 60 * 60;

    @DubboReference(check = false)
    private ISmsRpc smsRpc;

    @DubboReference(check = false)
    private IUserPhoneRpc userPhoneRpc;

    @PostMapping("/sendLoginCode")
    public WebResponseVO<Boolean> sendLoginCode(String phone) {
        if (!isValidPhone(phone)) {
            return WebResponseVO.fail("phone format is invalid");
        }

        MsgSendResultEnum sendResult = smsRpc.sendMessage(phone);
        if (sendResult == MsgSendResultEnum.SEND_SUCCESS) {
            return WebResponseVO.success("send sms success", true);
        }
        return WebResponseVO.fail(sendResult.getDesc());
    }

    @PostMapping("/login")
    public WebResponseVO<UserLoginVO> login(String phone, Integer code, HttpServletResponse response) {
        if (!isValidPhone(phone) || code == null) {
            return WebResponseVO.fail("phone or code is invalid");
        }

        Long userId = userPhoneRpc.getUserIdByPhone(phone);
        IUserPhoneRpcDTO loginResult = userId == null
                ? userPhoneRpc.register(phone, code)
                : userPhoneRpc.login(phone, code);

        if (loginResult == null || !loginResult.isIsLoginSuccess()) {
            String desc = loginResult == null ? "login failed" : loginResult.getDesc();
            return WebResponseVO.fail(desc);
        }

        writeTokenCookie(response, loginResult.getToken());
        UserLoginVO data = new UserLoginVO(loginResult.getUserId(), loginResult.getToken());
        return WebResponseVO.success("login success", data);
    }

    @GetMapping("/getUserIdByToken")
    public WebResponseVO<Long> getUserIdByToken(String token) {
        if (token == null || token.isBlank()) {
            return WebResponseVO.fail("token is invalid");
        }

        Long userId = userPhoneRpc.getUserIdByToken(token);
        if (userId == null) {
            return WebResponseVO.fail("token is invalid");
        }
        return WebResponseVO.success("query user id success", userId);
    }

    @LoginRequired
    @GetMapping("/currentUserId")
    public WebResponseVO<Long> currentUserId() {
        return WebResponseVO.success("current user query success", LoginUserContext.getUserId());
    }

    private void writeTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("qiyu_token", token);
        cookie.setPath("/");
        cookie.setMaxAge(TOKEN_COOKIE_MAX_AGE);
        response.addCookie(cookie);
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
