package org.qiyu.live.msg.interfac;

import org.qiyu.live.msg.dto.MsgCheckDTO;
import org.qiyu.live.msg.enums.MsgSendResultEnum;

public interface ISmsRpc {

    MsgSendResultEnum sendMessage(String phone);

    MsgCheckDTO checkLoginCode(String phone, Integer code);

    void insertOne(String phone, Integer code);
}
