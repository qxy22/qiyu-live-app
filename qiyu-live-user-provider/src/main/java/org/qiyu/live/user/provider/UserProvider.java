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
public class UserProvider{
    private static final Logger log = LoggerFactory.getLogger(UserProvider.class);

    @Resource
    private IUserTagService userTagService;

    @Resource
    private IUserService userService;
    public static void main(String[] args) {
        SpringApplication.run(UserProvider.class, args);
    }

}
