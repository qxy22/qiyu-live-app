package org.qiyu.live.api.vo;

public class WebResponseVO<T> {

    private Integer code;
    private String msg;
    private T data;

    public WebResponseVO() {
    }

    public WebResponseVO(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> WebResponseVO<T> success(T data) {
        return new WebResponseVO<>(200, "success", data);
    }

    public static <T> WebResponseVO<T> success(String msg, T data) {
        return new WebResponseVO<>(200, msg, data);
    }

    public static <T> WebResponseVO<T> fail(String msg) {
        return new WebResponseVO<>(500, msg, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
