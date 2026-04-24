# Docker Compose 启动说明

项目中已经为两个服务分别提供独立的 Docker Compose 文件：

| 服务 | Compose 文件 |
| --- | --- |
| qiyu-live-api | `qiyu-live-api/docker/docker-compose.yml` |
| qiyu-live-user-provider | `qiyu-live-user-provider/docker/docker-compose.yml` |

## 准备环境变量

进入对应 `docker` 目录，复制 `.env.example` 为 `.env`，并把 `NACOS_SERVER_ADDR` 改成你的真实局域网 IP。

```cmd
copy .env.example .env
```

示例：

```properties
NACOS_SERVER_ADDR=10.51.77.21:8848
NACOS_USERNAME=qiyu
NACOS_PASSWORD=qiyu
NACOS_NAMESPACE=qiyu-live-test
```

## 启动 user-provider

```cmd
cd /d D:\BaiduNetdiskDownload\qiyu-live-app\qiyu-live-user-provider\docker
copy .env.example .env
docker compose up -d
docker compose logs -f
```

## 启动 live-api

```cmd
cd /d D:\BaiduNetdiskDownload\qiyu-live-app\qiyu-live-api\docker
copy .env.example .env
docker compose up -d
docker compose logs -f
```

## 注意事项

Nacos 中的 Dubbo 地址也要使用同一个局域网 IP，例如：

```yaml
dubbo:
  registry:
    address: nacos://10.51.77.21:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu
```

容器中不能用 `127.0.0.1` 访问宿主机上的 Nacos、MySQL、Redis 或 RocketMQ。所有在宿主机运行的中间件，都建议在 Nacos 配置中使用宿主机局域网 IP。
