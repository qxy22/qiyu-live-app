package org.qiyu.live.api.vo;

import org.qiyu.live.user.DTO.UserDTO;

public class LoginResultVO {

    private boolean success;
    private String message;
    private String token;
    private UserDTO user;

    public LoginResultVO() {
    }

    public LoginResultVO(boolean success, String message, String token, UserDTO user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    public static LoginResultVO fail(String message) {
        return new LoginResultVO(false, message, null, null);
    }

    public static LoginResultVO success(String token, UserDTO user) {
        return new LoginResultVO(true, "登录成功", token, user);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}
