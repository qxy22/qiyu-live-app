package org.qiyu.live.user.DTO;

import java.io.Serializable;

public class IUserPhoneRpcDTO implements Serializable {
    private static final long serialVersionUID = 11111111L;

    private boolean isLoginSuccess;
    private Long userId;
    private String token;
    private String desc;


    public IUserPhoneRpcDTO() {
    }

    public IUserPhoneRpcDTO(boolean isLoginSuccess, Long userId, String token, String desc) {
        this.isLoginSuccess = isLoginSuccess;
        this.userId = userId;
        this.token = token;
        this.desc = desc;
    }

    /**
     * 获取
     * @return isLoginSuccess
     */
    public boolean isIsLoginSuccess() {
        return isLoginSuccess;
    }

    /**
     * 设置
     * @param isLoginSuccess
     */
    public void setIsLoginSuccess(boolean isLoginSuccess) {
        this.isLoginSuccess = isLoginSuccess;
    }

    /**
     * 获取
     * @return userId
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置
     * @param userId
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取
     * @return token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置
     * @param token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取
     * @return desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 设置
     * @param desc
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String toString() {
        return "IUserPhoneRpcDTO{isLoginSuccess = " + isLoginSuccess + ", userId = " + userId + ", token = " + token + ", desc = " + desc + "}";
    }
}
