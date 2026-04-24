package org.qiyu.live.user.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.qiyu.live.user.DTO.UserDTO;
import org.qiyu.live.user.interfac.IUserRpc;
import org.qiyu.live.user.provider.service.IUserService;

import java.util.List;

@DubboService
public class UserRpcImpl implements IUserRpc {

    @Resource
    private IUserService userService;

    @Override
    public UserDTO getUserById(Long userId) {
        return userService.getUserById(userId);
    }

    @Override
    public UserDTO getUserByPhone(String phone) {
        return userService.getUserByPhone(phone);
    }

    @Override
    public List<UserDTO> batchGetUserByIds(List<Long> userIds) {
        return userService.batchGetUserByIds(userIds);
    }

    @Override
    public boolean createUser(Long userId) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        return userService.createUser(userDTO);
    }

    @Override
    public boolean createUser(UserDTO userDTO) {
        return userService.createUser(userDTO);
    }

    @Override
    public boolean updateUser(UserDTO userDTO) {
        return userService.updateUser(userDTO);
    }

    @Override
    public boolean deleteUser(Long userId) {
        return userService.deleteUser(userId);
    }
}
