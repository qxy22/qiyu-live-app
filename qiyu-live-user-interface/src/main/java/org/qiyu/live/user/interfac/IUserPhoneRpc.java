package org.qiyu.live.user.interfac;

import org.qiyu.live.user.DTO.IUserPhoneRpcDTO;

public interface IUserPhoneRpc {

    /**
     * Phone login skeleton.
     *
     * @param phone phone number
     * @param code sms code
     * @return login result
     */
    IUserPhoneRpcDTO login(String phone, Integer code);

    /**
     * Phone register skeleton.
     *
     * @param phone phone number
     * @param code sms code
     * @return register result
     */
    IUserPhoneRpcDTO register(String phone, Integer code);

    /**
     * Query user id by phone.
     *
     * @param phone phone number
     * @return user id, or null when not found
     */
    Long getUserIdByPhone(String phone);

    /**
     * Query user id by token.
     *
     * @param token login token
     * @return user id, or null when token is invalid
     */
    Long getUserIdByToken(String token);

    /**
     * Bind phone to a user.
     *
     * @param userId user id
     * @param phone phone number
     * @return true when bind success
     */
    boolean bindPhone(Long userId, String phone);
}
