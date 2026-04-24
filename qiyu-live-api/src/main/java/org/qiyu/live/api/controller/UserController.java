package org.qiyu.live.api.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.qiyu.live.api.vo.LoginResultVO;
import org.qiyu.live.id.generate.interfac.IdBuilderRpc;
import org.qiyu.live.msg.dto.MsgCheckDTO;
import org.qiyu.live.msg.enums.MsgSendResultEnum;
import org.qiyu.live.msg.interfac.ISmsRpc;
import org.qiyu.live.user.DTO.UserDTO;
import org.qiyu.live.user.interfac.IUserRpc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final int USER_ID_BIZ_CODE = 1;
    private static final int TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    @DubboReference(check = false)
    private IUserRpc userRpc;

    @DubboReference(check = false)
    private ISmsRpc smsRpc;

    @DubboReference(check = false)
    private IdBuilderRpc idBuilderRpc;

    @GetMapping("/sendLoginCode")
    public MsgSendResultEnum sendLoginCode(String phone) {
        if (!isValidPhone(phone)) {
            return MsgSendResultEnum.MSG_PARAM_ERROR;
        }
        return smsRpc.sendMessage(phone);
    }

    @GetMapping("/login")
    public LoginResultVO login(String phone, Integer code, HttpServletResponse response) {
        if (!isValidPhone(phone) || code == null) {
            return LoginResultVO.fail("参数错误");
        }
        MsgCheckDTO checkDTO = smsRpc.checkLoginCode(phone, code);
        if (checkDTO == null || !checkDTO.isCheckStatus()) {
            String message = checkDTO == null ? "验证码校验失败" : checkDTO.getDesc();
            return LoginResultVO.fail(message);
        }

        UserDTO userDTO = userRpc.getUserByPhone(phone);
        if (userDTO == null) {
            userDTO = createUserByPhone(phone);
            boolean createSuccess = userRpc.createUser(userDTO);
            if (!createSuccess) {
                return LoginResultVO.fail("注册用户失败");
            }
        }

        String token = buildToken(userDTO.getUserId());
        writeTokenCookie(response, token);
        return LoginResultVO.success(token, userDTO);
    }

    @GetMapping("/getUserById/{userId}")
    public UserDTO getUserById(@PathVariable("userId") Long userId) {
        return userRpc.getUserById(userId);
    }

    @GetMapping("/batchGetUserByIds")
    public List<UserDTO> batchGetUserByIds(String userIds) {
        List<Long> userIdList = Arrays.stream(userIds.split(","))
                .map(Long::parseLong)
                .toList();
        return userRpc.batchGetUserByIds(userIdList);
    }

    @GetMapping("/updateUser")
    public boolean updateUser(Long userId, String nickName) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName(nickName);
        return userRpc.updateUser(userDTO);
    }

    @GetMapping("/deleteUser/{userId}")
    public boolean deleteUser(@PathVariable("userId") Long userId) {
        return userRpc.deleteUser(userId);
    }

    @GetMapping("/createUser")
    public boolean createUser(Long userId) {
        return userRpc.createUser(userId);
    }

    private UserDTO createUserByPhone(String phone) {
        Long userId = null;
        try {
            userId = idBuilderRpc.getSeqId(USER_ID_BIZ_CODE);
        } catch (Exception ignored) {
            // Local fallback keeps sms-login usable when the id service is not started.
        }
        if (userId == null) {
            userId = System.currentTimeMillis();
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setPhone(phone);
        userDTO.setNickName("用户" + phone.substring(phone.length() - 4));
        return userDTO;
    }

    private String buildToken(Long userId) {
        return userId + "-" + UUID.randomUUID();
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
