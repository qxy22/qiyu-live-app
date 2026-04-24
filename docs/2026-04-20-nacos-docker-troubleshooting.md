# 2026-04-20 Nacos + Docker 联调问题复盘

本文记录 `qiyu-live-api` 和 `qiyu-live-user-provider` 接入 Nacos 配置中心、Docker 镜像、Docker Compose 后遇到的问题与处理方式。

## 1. 本次目标

本次主要完成：

| 模块 | 目标 |
| --- | --- |
| `qiyu-live-user-provide[2026-04-20-nacos-docker-troubleshooting.md](2026-04-20-nacos-docker-troubleshooting.md)r` | 接入 Nacos Config，迁移服务配置和 ShardingSphere 配置 |
| `qiyu-live-api` | 接入 Nacos Config，迁移 API 和 Dubbo 消费端配置 |
| Docker | 为 API 和 user-provider 构建镜像 |
| Docker Compose | 为 API 和 user-provider 编写独立 compose 文件 |

最终运行链路：

```text
浏览器 / curl
-> localhost:8083
-> qiyu-live-api 容器
-> Dubbo 从 Nacos 找 IUserRpc provider
-> qiyu-live-user-provider 容器
-> ShardingSphere
-> MySQL 10.51.77.21:3306 / 3307
-> Redis 10.51.77.21:6379
-> RocketMQ 10.51.77.21:9876
```

## 2. Nacos Config 接入

已接入 Nacos Config 的模块：

```text
qiyu-live-api
qiyu-live-user-provider
```

核心依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

`qiyu-live-api` 读取：

```text
qiyu-live-api.yaml
```

`qiyu-live-user-provider` 读取：

```text
qiyu-live-user-provider.yaml
qiyu-db-sharding.yaml
```

`bootstrap.yml` 中通过 `spring.config.import` 导入：

```yaml
spring:
  config:
    import:
      - optional:nacos:qiyu-live-api.yaml
```

`optional` 表示 Nacos 暂时没有对应 DataId 时，不会直接阻止服务启动。

## 3. ShardingSphere 配置迁移

ShardingSphere 的 `dataSources`、`rules`、`props` 是独立 YAML 配置，不是 Spring Boot 配置属性，因此不能直接放在 `qiyu-live-user-provider.yaml` 的根节点中。

错误示例：

```yaml
dataSources:
  Mysql:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
```

实际方案：

1. Nacos 创建 `qiyu-db-sharding.yaml`
2. 应用启动早期从 Nacos 读取该 DataId
3. 写入本地临时文件
4. 将 `spring.datasource.url` 覆盖为：

```text
jdbc:shardingsphere:absolutepath:<临时文件路径>
```

如果 Nacos 中没有 `qiyu-db-sharding.yaml`，则回退到本地：

```text
jdbc:shardingsphere:classpath:qiyu-db-sharding.yaml
```

注意：ShardingSphere 的数据源和分片规则在数据源创建时加载，因此修改 `qiyu-db-sharding.yaml` 后需要重启 `qiyu-live-user-provider`。

## 4. Docker 镜像

已生成镜像：

```text
qiyu-live-api-docker:1.0-SNAPSHOT
qiyu-live-api-docker:latest
qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

API Dockerfile：

```dockerfile
FROM eclipse-temurin:17-jre-alpine

VOLUME /tmp
EXPOSE 8083

ADD qiyu-live-api-docker.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
```

user-provider Dockerfile：

```dockerfile
FROM eclipse-temurin:17-jre-alpine

VOLUME /tmp
EXPOSE 8085 9090

ADD qiyu-live-user-provider-docker.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
```

如果 Maven Docker 插件报：

```text
Connect to localhost:2375 failed: Connection refused
```

说明 Docker Desktop 没有开放 `tcp://localhost:2375`。可以选择开启 Docker Desktop 的 2375，或手动 `docker build`。

手动构建示例：

```cmd
cd /d D:\BaiduNetdiskDownload\qiyu-live-app
mvn -pl qiyu-live-api -am -DskipTests package
copy /Y qiyu-live-api\target\qiyu-live-api-docker.jar qiyu-live-api\docker\qiyu-live-api-docker.jar
docker build -t qiyu-live-api-docker:1.0-SNAPSHOT qiyu-live-api\docker
docker tag qiyu-live-api-docker:1.0-SNAPSHOT qiyu-live-api-docker:latest
```

## 5. Docker Compose

Compose 文件：

| 服务 | 文件 |
| --- | --- |
| qiyu-live-api | `qiyu-live-api/docker/docker-compose.yml` |
| qiyu-live-user-provider | `qiyu-live-user-provider/docker/docker-compose.yml` |

`.env` 内容：

```properties
NACOS_SERVER_ADDR=10.51.77.21:8848
NACOS_USERNAME=qiyu
NACOS_PASSWORD=qiyu
NACOS_NAMESPACE=qiyu-live-test
```

启动 user-provider：

```cmd
cd /d D:\BaiduNetdiskDownload\qiyu-live-app\qiyu-live-user-provider\docker
docker compose --env-file .env down
docker compose --env-file .env up -d
docker compose --env-file .env logs -f
```

启动 API：

```cmd
cd /d D:\BaiduNetdiskDownload\qiyu-live-app\qiyu-live-api\docker
docker compose --env-file .env down
docker compose --env-file .env up -d
docker compose --env-file .env logs -f
```

如果报：

```text
The "NACOS_SERVER_ADDR" variable is not set
```

说明没有 `.env` 文件，或没有在 compose 文件所在目录执行。

解决：

```cmd
copy .env.example .env
notepad .env
docker compose --env-file .env up -d
```

## 6. 容器网络注意事项

容器中的：

```text
127.0.0.1
localhost
```

指的是容器自己，不是 Windows 宿主机。

所以容器访问宿主机上的 Nacos、MySQL、Redis、RocketMQ 时，要使用宿主机局域网 IP：

```text
10.51.77.21
```

需要使用局域网 IP 的配置：

```yaml
spring:
  cloud:
    nacos:
      server-addr: 10.51.77.21:8848
  data:
    redis:
      host: 10.51.77.21

dubbo:
  registry:
    address: nacos://10.51.77.21:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu

rocketmq:
  name-server: 10.51.77.21:9876
```

MySQL：

```text
jdbc:mysql://10.51.77.21:3306/qiyu_live_user
jdbc:mysql://10.51.77.21:3307/qiyu_live_user
```

## 7. Nacos 3.x 端口和健康检查

当前 Nacos 版本是 3.1.1。

控制台地址：

```text
http://10.51.77.21:8080
```

客户端连接端口：

```text
10.51.77.21:8848
```

旧健康检查接口：

```text
/nacos/v1/console/health/readiness
```

在 Nacos 3.x 中会返回：

```text
410 Gone
```

应使用：

```cmd
curl http://10.51.77.21:8848/nacos/v3/console/health/readiness
```

Nacos 2.x/3.x 客户端还可能需要 gRPC 端口：

```text
9848
```

如果出现：

```text
Failed to create nacos config service client. Reason: server status check failed.
```

需要检查 `8848` 和 `9848` 是否对 Docker 容器可达。

Windows 防火墙放行：

```cmd
netsh advfirewall firewall add rule name="Nacos 8848" dir=in action=allow protocol=TCP localport=8848
netsh advfirewall firewall add rule name="Nacos 9848" dir=in action=allow protocol=TCP localport=9848
```

容器内测试：

```cmd
docker run --rm busybox sh -c "nc -zv 10.51.77.21 8848; nc -zv 10.51.77.21 9848"
```

## 8. Dubbo 配置迁移

`qiyu-live-api` 原本的 `dubbo.properties`：

```properties
dubbo.application.name=qiyu-live-api
dubbo.registry.address=nacos://127.0.0.1:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu
dubbo.client.timeout=3000
```

迁移到 Nacos：

```yaml
dubbo:
  application:
    name: qiyu-live-api
    qos-enable: false
  registry:
    address: nacos://10.51.77.21:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu
  consumer:
    timeout: 3000
    check: false
```

`check: false` 可以避免 API 启动时 provider 暂未上线导致启动失败，但调用接口时 provider 仍必须存在。

## 9. RocketMQ Topic 问题

曾出现：

```text
No route info of this topic: cache-delete-topic
```

原因：

```text
RocketMQ broker 上没有创建 cache-delete-topic
或 broker 没有注册到 nameserver
```

查看集群：

```cmd
mqadmin clusterList -n 10.51.77.21:9876
```

查看 topic：

```cmd
mqadmin topicList -n 10.51.77.21:9876
```

创建 topic：

```cmd
mqadmin updateTopic -n 10.51.77.21:9876 -c DefaultCluster -t cache-delete-topic
```

Nacos 配置：

```yaml
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

代码中已将 topic 和 consumer group 改成可配置。

## 10. Redis 连接问题

曾出现：

```text
Could not get a resource from the pool
```

原因：

```text
容器连不上 Redis
```

Redis 原配置：

```conf
bind 127.0.0.1 -::1
```

这会导致 Redis 只允许本机访问，容器访问失败。

开发环境可改成：

```conf
bind 0.0.0.0
protected-mode no
```

重启 Redis 后测试：

```cmd
docker run --rm busybox sh -c "nc -zv -w 3 10.51.77.21 6379 && echo REDIS_OK || echo REDIS_FAIL"
```

看到：

```text
REDIS_OK
```

才说明容器可以访问 Redis。

Nacos 配置：

```yaml
spring:
  data:
    redis:
      host: 10.51.77.21
      port: 6379
```

如果 Redis 有密码，需要增加：

```yaml
password: 你的密码
```

## 11. MySQL 权限问题

曾出现：

```text
Host 'DESKTOP-K1QKDBH' is not allowed to connect to this MySQL server
```

原因：

```text
MySQL 账号只允许 localhost 登录，不允许容器通过局域网 IP 登录
```

主库 3306 使用 root 密码 `123456` 时：

```sql
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

从库 3307 如果 root 没有密码：

```sql
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

测试主库：

```cmd
docker run --rm mysql:8.0 mysql -h10.51.77.21 -P3306 -uroot -p123456 -e "select 1;"
```

测试从库无密码：

```cmd
docker run --rm mysql:8.0 mysql -h10.51.77.21 -P3307 -uroot -e "select 1;"
```

`qiyu-db-sharding.yaml` 中，如果密码为空，不要写：

```yaml
password:
```

应该写：

```yaml
password: ""
```

## 12. 常用排查命令

查看容器：

```cmd
docker ps
docker ps -a
```

查看日志：

```cmd
docker logs -f qiyu-live-api
docker logs -f qiyu-live-user-provider
```

筛选 API 错误：

```cmd
docker logs qiyu-live-api --tail 300 | findstr /i "error exception caused dubbo provider rpc failed timeout refused"
```

筛选 provider 错误：

```cmd
docker logs qiyu-live-user-provider --tail 300 | findstr /i "error exception caused mysql redis nacos rocket failed"
```

查看容器环境变量：

```cmd
docker inspect qiyu-live-user-provider --format "{{range .Config.Env}}{{println .}}{{end}}" | findstr NACOS
```

查看端口占用：

```cmd
netstat -ano | findstr :8085
netstat -ano | findstr :9090
netstat -ano | findstr :6379
netstat -ano | findstr :3306
netstat -ano | findstr :9876
```

启动/停止 compose：

```cmd
docker compose --env-file .env down
docker compose --env-file .env up -d
docker compose --env-file .env logs -f
```

## 13. 验证接口

启动顺序：

```text
Nacos / MySQL / Redis / RocketMQ
-> qiyu-live-user-provider
-> qiyu-live-api
```

验证接口：

```cmd
curl http://localhost:8083/user/getUserById/1113
```

如果返回用户 JSON，说明链路打通。

如果返回 500：

1. 先看 API 日志，判断是否 Dubbo 找不到 provider
2. 再看 user-provider 日志，判断是否 MySQL、Redis、RocketMQ 或 Nacos 问题
3. 确认 Nacos 服务列表里有 API 和 provider
4. 确认所有宿主机中间件地址都使用 `10.51.77.21`，不要使用 `localhost` 或 `127.0.0.1`
