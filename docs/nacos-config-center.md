# Nacos 配置中心接入说明

当前已有两个服务接入 Nacos Config：

| DataId | 作用 |
| --- | --- |
| `qiyu-live-api.yaml` | API 网关/接口层服务配置 |
| `qiyu-live-user-provider.yaml` | 用户服务、Dubbo、Redis、RocketMQ 等服务配置 |
| `qiyu-db-sharding.yaml` | user-provider 的 ShardingSphere JDBC 分库分表、读写分离和真实数据源配置 |

## Nacos 连接参数

默认连接参数如下，可通过环境变量覆盖：

| 参数 | 默认值 | 环境变量 |
| --- | --- | --- |
| Nacos 地址 | `10.51.77.21:8848` | `NACOS_SERVER_ADDR` |
| 命名空间 | `qiyu-live-test` | `NACOS_NAMESPACE` |
| 用户名 | `qiyu` | `NACOS_USERNAME` |
| 密码 | `qiyu` | `NACOS_PASSWORD` |

## qiyu-live-api.yaml 示例

```yaml
server:
  port: 8083

spring:
  application:
    name: qiyu-live-api
  cloud:
    nacos:
      username: qiyu
      password: qiyu
      server-addr: 10.51.77.21:8848
      discovery:
        namespace: qiyu-live-test

dubbo:
  application:
    name: qiyu-live-api
  registry:
    address: nacos://10.51.77.21:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu
  consumer:
    timeout: 3000
```

## qiyu-live-user-provider.yaml 示例

该 DataId 放服务启动配置。`spring.datasource.url` 可以保留 classpath 写法；如果 Nacos 里存在 `qiyu-db-sharding.yaml`，启动时会自动覆盖成 `jdbc:shardingsphere:absolutepath:...`。

```yaml
server:
  port: 8085

spring:
  main:
    allow-bean-definition-overriding: true
    allow-circular-references: true
  data:
    redis:
      host: 10.51.77.21
      port: 6379
      database: 0
      timeout: 10s
      lettuce:
        pool:
          max-active: 200
          max-idle: 20
          min-idle: 5
          max-wait: 3000ms
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:qiyu-db-sharding.yaml

dubbo:
  application:
    name: qiyu-live-user-application
    qos-enable: false
  registry:
    address: nacos://10.51.77.21:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu
  protocol:
    name: dubbo
    port: 9090
  provider:
    timeout: 3000
  scan:
    base-packages: org.qiyu.live.user.provider.rpc

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: org.qiyu.live.user.provider.dao.po

logging:
  level:
    root: info
    org.qiyu: info
    org.apache.shardingsphere: info

rocketmq:
  name-server: 10.51.77.21:9876
  producer:
    group: cache-delete-producer-group
    send-message-timeout: 5000
    retry-times-when-send-failed: 2

qiyu:
  rocketmq:
    cache-delete:
      topic: cache-delete-topic
      consumer-group: cache-delete-consumer-group
```

## qiyu-db-sharding.yaml 示例

该 DataId 放 ShardingSphere 独立配置，内容可以直接复制本地 `qiyu-live-user-provider/src/main/resources/qiyu-db-sharding.yaml`。

```yaml
databaseName: logic_db
dataSources:
  Mysql:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://10.51.77.21:3306/qiyu_live_user?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    minimumIdle: 15
    maximumPoolSize: 300
    idleTimeout: 60000
    connectionTimeout: 4000
    maxLifetime: 60000

  mysql_read:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://10.51.77.21:3306/qiyu_live_user?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    minimumIdle: 15
    maximumPoolSize: 300
    idleTimeout: 60000
    connectionTimeout: 4000
    maxLifetime: 60000

rules:
  - !READWRITE_SPLITTING
    dataSourceGroups:
      readwrite_ds:
        writeDataSourceName: Mysql
        readDataSourceNames:
          - mysql_read
        transactionalReadQueryStrategy: PRIMARY
        loadBalancerName: random
    loadBalancers:
      random:
        type: RANDOM

  - !SINGLE
      defaultDataSource: readwrite_ds

  - !SHARDING
    tables:
      t_user:
        actualDataNodes: readwrite_ds.t_user_${(0..99).collect(){it.toString().padLeft(2,'0')}}
        tableStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: t_user-inline
      t_user_tag:
        actualDataNodes: readwrite_ds.t_user_tag_${(0..99).collect(){it.toString().padLeft(2,'0')}}
        tableStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: t_user_tag-inline
      t_user_phone:
        actualDataNodes: readwrite_ds.t_user_phone_${(0..99).collect(){it.toString().padLeft(2,'0')}}
        tableStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: t_user_phone-inline
    shardingAlgorithms:
      t_user-inline:
        type: INLINE
        props:
          algorithm-expression: t_user_${(user_id % 100).toString().padLeft(2,'0')}
      t_user_tag-inline:
        type: INLINE
        props:
          algorithm-expression: t_user_tag_${(user_id % 100).toString().padLeft(2,'0')}
      t_user_phone-inline:
        type: INLINE
        props:
          algorithm-expression: t_user_phone_${(user_id % 100).toString().padLeft(2,'0')}

props:
  sql-show: true
```

## ShardingSphere 工作方式

启动 `qiyu-live-user-provider` 时，`NacosShardingSphereConfigEnvironmentPostProcessor` 会先读取 Nacos 中的 `qiyu-db-sharding.yaml`，写入系统临时目录，再把 `spring.datasource.url` 覆盖为 `jdbc:shardingsphere:absolutepath:<临时文件路径>`。

如果 Nacos 中没有 `qiyu-db-sharding.yaml`，服务会回退到 `qiyu-live-user-provider.yaml` 中配置的 `jdbc:shardingsphere:classpath:qiyu-db-sharding.yaml`，继续使用本地文件。

ShardingSphere 的数据源和分片规则是在数据源创建时加载的，所以修改 `qiyu-db-sharding.yaml` 后需要重启 `qiyu-live-user-provider` 才会生效。

## RocketMQ Topic

如果启动或更新用户信息时报：

```text
No route info of this topic: cache-delete-topic
```

说明当前 RocketMQ broker 上没有创建 `cache-delete-topic`，或者服务连接的 `rocketmq.name-server` 不是 broker 实际注册到的 nameserver。

可以用 RocketMQ 自带的 `mqadmin` 创建 topic：

```bash
mqadmin updateTopic -n 10.51.77.21:9876 -c DefaultCluster -t cache-delete-topic
```

如果 broker 集群名不是 `DefaultCluster`，先查看集群信息：

```bash
mqadmin clusterList -n 10.51.77.21:9876
```

开发环境也可以在 broker 配置中开启自动创建 topic：

```properties
autoCreateTopicEnable=true
```

生产环境建议显式创建 topic，不建议依赖自动创建。
