package org.qiyu.live.msg.enums;

public enum MsgSendResultEnum {

    SEND_SUCCESS(0, "成功"),
    SEND_FAIL(1, "发送失败"),
    MSG_PARAM_ERROR(2, "消息格式异常");

    private final int code;
    private final String desc;

    MsgSendResultEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
