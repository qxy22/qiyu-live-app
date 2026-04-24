package org.qiyu.live.user.provider.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads ShardingSphere YAML from Nacos before Spring creates the datasource.
 */
public class NacosShardingSphereConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "nacosShardingSphereConfig";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("qiyu.sharding.nacos.enabled", Boolean.class, true)) {
            return;
        }
        String dataId = environment.getProperty("qiyu.sharding.nacos.data-id", "qiyu-db-sharding.yaml");
        String group = environment.getProperty("qiyu.sharding.nacos.group", "DEFAULT_GROUP");
        String content = getNacosConfig(environment, dataId, group);
        if (!StringUtils.hasText(content)) {
            return;
        }
        Path configPath = writeShardingConfig(environment, dataId, content);
        String shardingSphereUrl = "jdbc:shardingsphere:absolutepath:" + configPath.toString().replace('\\', '/');

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.driver-class-name", "org.apache.shardingsphere.driver.ShardingSphereDriver");
        properties.put("spring.datasource.url", shardingSphereUrl);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private String getNacosConfig(ConfigurableEnvironment environment, String dataId, String group) {
        String serverAddr = firstNonBlank(
                environment.getProperty("qiyu.sharding.nacos.server-addr"),
                environment.getProperty("spring.cloud.nacos.config.server-addr"),
                environment.getProperty("spring.cloud.nacos.server-addr")
        );
        if (!StringUtils.hasText(serverAddr)) {
            return null;
        }
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        putIfHasText(properties, "namespace", firstNonBlank(
                environment.getProperty("qiyu.sharding.nacos.namespace"),
                environment.getProperty("spring.cloud.nacos.config.namespace"),
                environment.getProperty("spring.cloud.nacos.discovery.namespace")
        ));
        putIfHasText(properties, "username", firstNonBlank(
                environment.getProperty("qiyu.sharding.nacos.username"),
                environment.getProperty("spring.cloud.nacos.username")
        ));
        putIfHasText(properties, "password", firstNonBlank(
                environment.getProperty("qiyu.sharding.nacos.password"),
                environment.getProperty("spring.cloud.nacos.password")
        ));
        try {
            ConfigService configService = NacosFactory.createConfigService(properties);
            long timeoutMs = environment.getProperty("qiyu.sharding.nacos.timeout-ms", Long.class, 3000L);
            return configService.getConfig(dataId, group, timeoutMs);
        } catch (NacosException e) {
            return null;
        }
    }

    private Path writeShardingConfig(ConfigurableEnvironment environment, String dataId, String content) {
        String defaultDir = Path.of(System.getProperty("java.io.tmpdir"), "qiyu-live-user-provider").toString();
        String outputDir = environment.getProperty("qiyu.sharding.nacos.output-dir", defaultDir);
        Path configPath = Path.of(outputDir, dataId).toAbsolutePath().normalize();
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, content, StandardCharsets.UTF_8);
            return configPath;
        } catch (IOException e) {
            throw new IllegalStateException("Write Nacos ShardingSphere config failed: " + configPath, e);
        }
    }

    private void putIfHasText(Properties properties, String key, String value) {
        if (StringUtils.hasText(value)) {
            properties.setProperty(key, value);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
