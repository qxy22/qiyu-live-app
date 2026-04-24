package org.qiyu.live.id.generate.provider;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.qiyu.live.id.generate.provider.service.IdGenerateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ID 生成服务启动类。
 *
 * <p>启动后会连接 Nacos，并通过 Dubbo 暴露 IdBuilderRpc 服务。</p>
 */
@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class IdGenerateProvider{

    private static final Logger LOGGER = LoggerFactory.getLogger(IdGenerateProvider.class);


    @Resource
    private IdGenerateService idGenerateService;

    public static void main(String[] args) {
        // 启动 Spring Boot 容器，完成 bean 扫描、Dubbo 服务暴露和 Nacos 注册。
        SpringApplication.run(IdGenerateProvider.class, args);
    }

//    @Override
//    public void run(String... args) throws Exception {
//        // 循环 1300 次
//        for (int i = 0; i < 1300; i++) {
//            // 调用 ID 生成服务，传入业务类型 1，生成一个唯一 ID
//            Long id = idGenerateService.getUnSeqId(1);
//            // 打印生成的 ID
//            System.out.println(id);
//
//        }
//    }

    private void printTestMessage(String message) {
        LOGGER.info(message);
        System.out.println("[ID-GENERATE-TEST] " + message);
    }
}
