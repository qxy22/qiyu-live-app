package org.qiyu.live.msg.provider.service;

import org.qiyu.live.msg.dto.MsgCheckDTO;
import org.qiyu.live.msg.enums.MsgSendResultEnum;

public interface ISmsService {

    MsgSendResultEnum sendMessage(String phone);

    MsgCheckDTO checkLoginCode(String phone, Integer code);

    void insertOne(String phone, Integer code);
}
