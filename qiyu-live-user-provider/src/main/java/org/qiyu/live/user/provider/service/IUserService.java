package org.qiyu.live.user.provider.service;

import org.qiyu.live.user.DTO.UserDTO;

import java.util.List;

public interface IUserService {

    UserDTO getUserById(Long userId);

    UserDTO getUserByPhone(String phone);

    /**
     * 批量获取用户信息（高并发优化版）
     * 核心策略：Redis缓存 + 多线程分表查询
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表（按传入顺序）
     */
    List<UserDTO> batchGetUserByIds(List<Long> userIds);

    /**
     * 更新用户信息
     * @param userDTO 用户信息
     */
    boolean updateUser(UserDTO userDTO);

    /**
     * 删除用户
     * @param userId 用户ID
     */
    boolean deleteUser(Long userId);

    /**
     * 插入用户
     * @param userDTO 用户信息
     * @return 插入的用户信息
     */
    boolean createUser(UserDTO userDTO);

}
