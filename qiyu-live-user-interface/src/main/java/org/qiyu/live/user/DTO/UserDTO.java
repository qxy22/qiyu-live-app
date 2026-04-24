package org.qiyu.live.user.DTO;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class UserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 88888888L;
    private Long userId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 真实姓名
     */
    private String trueName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别（0-女，1-男）
     */
    private Integer sex;

    /**
     * 工作城市编码
     */
    private Integer workCity;

    /**
     * 出生城市编码
     */
    private Integer bornCity;

    /**
     * 出生日期
     */
    private Date bornDate;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    public UserDTO() {
    }

    public UserDTO(Long userId, String nickName, String trueName, String avatar, Integer sex, Integer workCity, Integer bornCity, Date bornDate, Date createTime, Date updateTime) {
        this.userId = userId;
        this.nickName = nickName;
        this.trueName = trueName;
        this.avatar = avatar;
        this.sex = sex;
        this.workCity = workCity;
        this.bornCity = bornCity;
        this.bornDate = bornDate;
        this.createTime = createTime;
        this.updateTime = updateTime;
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
     * @return nickName
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * 设置
     * @param nickName
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * 获取
     * @return trueName
     */
    public String getTrueName() {
        return trueName;
    }

    /**
     * 设置
     * @param trueName
     */
    public void setTrueName(String trueName) {
        this.trueName = trueName;
    }

    /**
     * 获取
     * @return avatar
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * 设置
     * @param avatar
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取
     * @return sex
     */
    public Integer getSex() {
        return sex;
    }

    /**
     * 设置
     * @param sex
     */
    public void setSex(Integer sex) {
        this.sex = sex;
    }

    /**
     * 获取
     * @return workCity
     */
    public Integer getWorkCity() {
        return workCity;
    }

    /**
     * 设置
     * @param workCity
     */
    public void setWorkCity(Integer workCity) {
        this.workCity = workCity;
    }

    /**
     * 获取
     * @return bornCity
     */
    public Integer getBornCity() {
        return bornCity;
    }

    /**
     * 设置
     * @param bornCity
     */
    public void setBornCity(Integer bornCity) {
        this.bornCity = bornCity;
    }

    /**
     * 获取
     * @return bornDate
     */
    public Date getBornDate() {
        return bornDate;
    }

    /**
     * 设置
     * @param bornDate
     */
    public void setBornDate(Date bornDate) {
        this.bornDate = bornDate;
    }

    /**
     * 获取
     * @return createTime
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 设置
     * @param createTime
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取
     * @return updateTime
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置
     * @param updateTime
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String toString() {
        return "UserDTO{serialVersionUID = " + serialVersionUID + ", userId = " + userId + ", nickName = " + nickName + ", trueName = " + trueName + ", avatar = " + avatar + ", phone = " + phone + ", sex = " + sex + ", workCity = " + workCity + ", bornCity = " + bornCity + ", bornDate = " + bornDate + ", createTime = " + createTime + ", updateTime = " + updateTime + "}";
    }
}
