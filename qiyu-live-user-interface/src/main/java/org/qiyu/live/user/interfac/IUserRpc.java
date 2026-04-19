package org.qiyu.live.user.interfac;

import org.qiyu.live.user.DTO.UserDTO;

import java.util.List;

public interface IUserRpc {

    /**
     * 根据用户ID查询用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long userId);

    /**
     * 批量查询用户信息（高并发优化版）
     * 核心策略：Redis缓存 + 多线程分表查询
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表（按传入顺序）
     */
    List<UserDTO> batchGetUserByIds(List<Long> userIds);

    /**
     * 插入用户
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean createUser(Long userId);

    /**
     * 更新用户信息
     * @param userDTO 用户信息
     * @return 是否成功
     */
    boolean updateUser(UserDTO userDTO);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long userId);
}
