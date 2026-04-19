package org.qiyu.live.api.controller;

import org.apache.dubbo.config.annotation.DubboReference;
import org.qiyu.live.user.DTO.UserDTO;
import org.qiyu.live.user.interfac.IUserRpc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @DubboReference
    private IUserRpc userRpc;

    @GetMapping("/getUserById/{userId}")
    public UserDTO getUserById(@PathVariable("userId") Long userId) {
        return userRpc.getUserById(userId);
    }

    @GetMapping("/batchGetUserByIds")
    public List<UserDTO> batchGetUserByIds(String userIds) {
        List<Long> userIdList = Arrays.stream(userIds.split(","))
                .map(Long::parseLong)
                .toList();
        return userRpc.batchGetUserByIds(userIdList);
    }

    @GetMapping("/updateUser")
    public boolean updateUser(Long userId, String nickName) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName(nickName);
        return userRpc.updateUser(userDTO);
    }

    @GetMapping("/deleteUser/{userId}")
    public boolean deleteUser(@PathVariable("userId") Long userId) {
        return userRpc.deleteUser(userId);
    }

    @GetMapping("/createUser")
    public boolean createUser(Long userId) {
        return userRpc.createUser(userId);
    }
}
