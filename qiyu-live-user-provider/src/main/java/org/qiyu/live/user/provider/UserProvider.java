package org.qiyu.live.user.provider;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.qiyu.live.user.DTO.UserDTO;
import org.qiyu.live.user.constants.UserTagsEnum;
import org.qiyu.live.user.provider.service.IUserService;
import org.qiyu.live.user.provider.service.IUserTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class UserProvider implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(UserProvider.class);

    @Resource
    private IUserTagService userTagService;

    @Resource
    private IUserService userService;
    public static void main(String[] args) {
        SpringApplication.run(UserProvider.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        long userId = 1114L;
        UserDTO userDTO = userService.getUserById(userId);
        System.out.println(userDTO);
        System.out.println("-----------------------------");
        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_VIP));
//        userDTO.setNickName("测试用户");
//        userService.updateUser(userDTO);
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.setTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.cancelTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_VIP));



//        System.out.println(userTagService.setTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.setTag(userId, UserTagsEnum.IS_OLD_USER));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_OLD_USER));
//        System.out.println(userTagService.setTag(userId, UserTagsEnum.IS_ANCHOR));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_ANCHOR));
//        System.out.println("-----------------------------");
//        System.out.println(userTagService.cancelTag(userId, UserTagsEnum.IS_ANCHOR));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_ANCHOR));
//        System.out.println(userTagService.cancelTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_VIP));
//        System.out.println(userTagService.cancelTag(userId, UserTagsEnum.IS_OLD_USER));
//        System.out.println(userTagService.containTag(userId, UserTagsEnum.IS_OLD_USER));
    }
}
