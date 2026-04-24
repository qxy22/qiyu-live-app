package org.qiyu.live.api.vo;

public class UserLoginVO {

    private Long userId;
    private String token;

    public UserLoginVO() {
    }

    public UserLoginVO(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
