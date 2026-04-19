package org.idea.qiyu.live.framework.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @Author idea
 * @Date: Created in 18:06 2023/5/7
 * @Description
 */
@Configuration
public class ShardingJdbcDatasourceAutoInitConnectionConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardingJdbcDatasourceAutoInitConnectionConfig.class);

    @Bean
    public ApplicationRunner runner(DataSource dataSource) {
        return args -> {
            LOGGER.info("dataSource: {}", dataSource);
            // 手动触发下连接池的连接创建
            try (Connection connection = dataSource.getConnection()) {
                // 这里不需要做任何操作，获取连接本身就会触发连接池创建
            } catch (Exception e) {
                LOGGER.error("Get connection error, {}", e.getMessage(), e);
            }
        };
    }
}