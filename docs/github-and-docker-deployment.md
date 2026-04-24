# qiyu-live-app GitHub 与 Docker 部署说明

本文档介绍如何将 `qiyu-live-app` 项目部署到 GitHub，以及如何将 Spring Boot 模块制作成 Docker 镜像并启动容器。当前项目已完成容器化的是 `qiyu-live-user-provider` 模块。

## 一、部署到 GitHub 的作用

把项目推送到 GitHub 的主要作用：

1. 代码备份：本地电脑出问题时，远程仓库仍然保留代码。
2. 版本管理：每次提交都会形成历史记录，方便回退和查看修改。
3. 团队协作：其他人可以拉取代码、创建分支、提交 Pull Request。
4. 部署基础：服务器、CI/CD 工具可以从 GitHub 拉取代码后自动构建和部署。

需要注意：GitHub 保存的是项目源码，不等于 Docker 镜像已经部署成功。Docker 镜像和容器需要在本机、服务器或 CI/CD 环境中单独构建和运行。

## 二、首次推送项目到 GitHub

### 1. 进入项目目录

```powershell
cd /d D:\BaiduNetdiskDownload\qiyu-live-app
```

如果使用 PowerShell，也可以执行：

```powershell
Set-Location D:\BaiduNetdiskDownload\qiyu-live-app
```

### 2. 确认当前目录是 Git 仓库

```powershell
git status
```

如果能看到分支和文件状态，说明当前目录正确。

如果出现：

```text
fatal: not a git repository
```

说明当前命令行不在项目目录，需要重新进入 `D:\BaiduNetdiskDownload\qiyu-live-app`。

### 3. 配置远程仓库地址

如果远程仓库地址还没有配置，执行：

```powershell
git remote add origin https://github.com/qxy22/qiyu-live-app.git
```

检查远程地址：

```powershell
git remote -v
```

正常输出类似：

```text
origin  https://github.com/qxy22/qiyu-live-app.git (fetch)
origin  https://github.com/qxy22/qiyu-live-app.git (push)
```

如果提示：

```text
remote origin already exists
```

说明已经配置过远程地址，可以改用：

```powershell
git remote set-url origin https://github.com/qxy22/qiyu-live-app.git
```

### 4. 添加 `.gitignore`

项目中不要把编译产物和 IDE 配置提交到 GitHub。根目录 `.gitignore` 建议包含：

```gitignore
target/
.idea/
.vscode/
*.iml
qiyu-live-user-provider/docker/*.jar
```

其中：

- `target/` 是 Maven 编译输出目录。
- `.idea/` 和 `.vscode/` 是本地 IDE 配置。
- `qiyu-live-user-provider/docker/*.jar` 是 Docker 构建时临时复制进去的 jar 包，文件很大，不适合提交到 GitHub。

### 5. 提交代码

```powershell
git add .
git commit -m "add project source and docker deployment"
```

注意：必须使用 `git add .`，不能只写 `git add`。

如果只写：

```powershell
git add
```

Git 不会添加任何文件，并会提示：

```text
Nothing specified, nothing added.
```

### 6. 推送到 GitHub

当前项目使用的是 `master` 分支，可以执行：

```powershell
git push -u origin master
```

推送成功后，刷新 GitHub 页面。如果页面默认显示 `main` 分支，而代码推到了 `master` 分支，需要在 GitHub 页面左上角的分支下拉框切换到 `master`。

如果希望 GitHub 默认展示 `master` 分支，可以在仓库设置中修改：

```text
Settings -> Branches -> Default branch -> master
```

## 三、日常 GitHub 拉取和推送

### 拉取远程最新代码

```powershell
git pull
```

### 提交自己的修改

```powershell
git add .
git commit -m "说明本次修改内容"
git push
```

### 查看当前状态

```powershell
git status
```

如果显示：

```text
nothing to commit, working tree clean
```

说明本地没有未提交的修改。

## 四、Docker 容器化的作用

Docker 容器化就是把应用程序、运行环境、启动命令等内容打包成一个镜像，然后通过容器运行。

容器化的主要作用：

1. 环境一致：开发、测试、服务器使用同一个镜像，减少“我电脑能跑，服务器不能跑”的问题。
2. 部署简单：服务器只要安装 Docker，就可以用一条命令启动应用。
3. 隔离性好：每个服务运行在自己的容器中，端口、依赖、进程相对独立。
4. 易于迁移：镜像可以推送到镜像仓库，在其他机器拉取后直接运行。
5. 方便扩展：后续可以配合 Docker Compose、Kubernetes 等工具管理多个服务。

需要注意：Docker 容器化不等于解决所有配置问题。项目依赖的 Nacos、MySQL、Redis、RocketMQ 等外部服务仍然需要正确配置地址和网络。

## 五、本项目 Docker 容器化结构

当前容器化模块：

```text
qiyu-live-user-provider
```

关键文件：

```text
qiyu-live-user-provider/pom.xml
qiyu-live-user-provider/src/main/resources/application.yml
qiyu-live-user-provider/docker/Dockerfile
```

当前应用端口：

```yaml
server:
  port: 8085
```

当前 Dubbo 端口：

```yaml
dubbo:
  protocol:
    name: dubbo
    port: 9090
```

当前 Dockerfile：

```dockerfile
FROM eclipse-temurin:17-jre-alpine

VOLUME /tmp
EXPOSE 8085 9090

ADD qiyu-live-user-provider-docker.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
```

说明：

- `FROM eclipse-temurin:17-jre-alpine`：使用 Java 17 运行环境。
- `EXPOSE 8085 9090`：声明容器内服务端口。
- `ADD qiyu-live-user-provider-docker.jar app.jar`：把 jar 包复制进镜像。
- `ENTRYPOINT`：容器启动时执行 `java -jar /app.jar`。

## 六、构建 Docker 镜像

### 1. 确认 Docker Desktop 已启动

```powershell
docker info
```

如果能输出 Docker Server 信息，说明 Docker 可用。

如果出现权限问题，例如：

```text
permission denied while trying to connect to the docker API
```

可以尝试：

1. 以管理员身份重新打开终端。
2. 确认 Docker Desktop 已经启动完成。
3. 将当前 Windows 用户加入 `docker-users` 用户组后重启电脑。

### 2. 打包 Spring Boot jar

在项目根目录执行：

```powershell
mvn -pl qiyu-live-user-provider -am package -DskipTests
```

成功后会生成：

```text
qiyu-live-user-provider/target/qiyu-live-user-provider-docker.jar
```

### 3. 复制 jar 到 Docker 构建目录

```powershell
copy qiyu-live-user-provider\target\qiyu-live-user-provider-docker.jar qiyu-live-user-provider\docker\
```

PowerShell 中也可以使用：

```powershell
Copy-Item -LiteralPath "qiyu-live-user-provider\target\qiyu-live-user-provider-docker.jar" -Destination "qiyu-live-user-provider\docker\" -Force
```

### 4. 构建镜像

```powershell
docker build -t qiyu-live-user-provider-docker:1.0-SNAPSHOT qiyu-live-user-provider\docker
```

构建成功后会看到类似：

```text
naming to docker.io/library/qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

查看镜像：

```powershell
docker images
```

如果能看到下面内容，说明镜像构建成功：

```text
qiyu-live-user-provider-docker   1.0-SNAPSHOT
```

## 七、启动 Docker 容器

由于宿主机 `9090` 端口可能被占用，建议把宿主机的 `19090` 映射到容器内的 `9090`。

启动容器：

```powershell
docker run -d --name qiyu-live-user-provider-01 -p 8085:8085 -p 19090:9090 qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

端口说明：

```text
宿主机 8085  -> 容器 8085，Spring Boot HTTP 端口
宿主机 19090 -> 容器 9090，Dubbo 端口
```

查看运行中的容器：

```powershell
docker ps
```

如果看到：

```text
qiyu-live-user-provider-01   Up
```

说明容器启动成功。

查看所有容器，包括已退出的容器：

```powershell
docker ps -a
```

查看日志：

```powershell
docker logs qiyu-live-user-provider-01
```

实时查看日志：

```powershell
docker logs -f qiyu-live-user-provider-01
```

## 八、停止、启动、删除容器

停止容器：

```powershell
docker stop qiyu-live-user-provider-01
```

重新启动已经存在的容器：

```powershell
docker start qiyu-live-user-provider-01
```

删除容器：

```powershell
docker rm qiyu-live-user-provider-01
```

如果容器正在运行，需要先停止再删除：

```powershell
docker stop qiyu-live-user-provider-01
docker rm qiyu-live-user-provider-01
```

## 九、重新构建并部署

当修改了 Java 代码、`application.yml` 或 Dockerfile 后，需要重新打包、复制 jar、构建镜像、重启容器。

完整命令：

```powershell
cd /d D:\BaiduNetdiskDownload\qiyu-live-app

mvn -pl qiyu-live-user-provider -am package -DskipTests

copy qiyu-live-user-provider\target\qiyu-live-user-provider-docker.jar qiyu-live-user-provider\docker\

docker build -t qiyu-live-user-provider-docker:1.0-SNAPSHOT qiyu-live-user-provider\docker

docker stop qiyu-live-user-provider-01
docker rm qiyu-live-user-provider-01

docker run -d --name qiyu-live-user-provider-01 -p 8085:8085 -p 19090:9090 qiyu-live-user-provider-docker:1.0-SNAPSHOT

docker ps
```

## 十、常见问题

### 1. GitHub 页面看不到代码

可能原因：代码推到了 `master` 分支，但 GitHub 页面默认显示 `main` 分支。

解决方式：在 GitHub 页面左上角分支下拉框切换到 `master`。

### 2. `fatal: not a git repository`

原因：命令行不在项目目录。

解决：

```powershell
cd /d D:\BaiduNetdiskDownload\qiyu-live-app
```

### 3. `git add` 后提示 Nothing specified

原因：只执行了 `git add`，没有指定要添加的文件。

解决：

```powershell
git add .
```

### 4. Docker 构建时拉取基础镜像失败

错误示例：

```text
failed to resolve source metadata
EOF
```

可能原因：Docker Hub 或镜像源网络不稳定。

解决：

1. 确认网络可以访问 Docker Hub。
2. 在 Docker Desktop 中检查镜像源配置。
3. 删除失效的镜像源，例如 `registry.docker-cn.com`。
4. 重新拉取基础镜像：

```powershell
docker pull eclipse-temurin:17-jre-alpine
```

### 5. 端口被占用

错误示例：

```text
ports are not available
Only one usage of each socket address is normally permitted
```

原因：宿主机端口已经被其他程序使用。

解决方式一：换宿主机映射端口。例如宿主机 `19090` 映射到容器 `9090`：

```powershell
docker run -d --name qiyu-live-user-provider-01 -p 8085:8085 -p 19090:9090 qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

解决方式二：查找占用端口的进程：

```powershell
netstat -ano | findstr :9090
tasklist | findstr 进程PID
```

确认可以关闭后：

```powershell
taskkill /PID 进程PID /F
```

### 6. 容器启动后马上退出

先查看日志：

```powershell
docker logs qiyu-live-user-provider-01
```

常见原因：

1. Nacos 地址无法访问。
2. MySQL 地址无法访问。
3. Redis 地址无法访问。
4. RocketMQ 地址无法访问。
5. 配置文件中的 `127.0.0.1` 在容器中指向容器自己，不是宿主机。

如果服务部署在宿主机，容器访问宿主机通常不能直接使用 `127.0.0.1`，需要改成宿主机 IP、Docker 网络服务名，或使用合适的 Docker 网络配置。

对应解决方案：

1. 先确认宿主机上的依赖服务是否已经启动。

```powershell
netstat -ano | findstr :8848
netstat -ano | findstr :3306
netstat -ano | findstr :6379
netstat -ano | findstr :9876
netstat -ano | findstr :10911
```

端口含义：

```text
8848  -> Nacos
3306  -> MySQL
6379  -> Redis
9876  -> RocketMQ NameServer
10911 -> RocketMQ Broker
```

如果某个端口没有 `LISTENING`，说明对应服务没有启动，需要先启动该服务。

2. 容器访问 Windows 宿主机服务时，优先使用：

```text
host.docker.internal
```

不要在容器配置里直接使用：

```text
127.0.0.1
localhost
```

3. 启动容器时用环境变量覆盖 Nacos、Redis、RocketMQ 地址。

CMD 写法：

```cmd
docker rm qiyu-live-user-provider-01

docker run -d --name qiyu-live-user-provider-01 -p 18085:8085 -p 19090:9090 -e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848 -e DUBBO_REGISTRY_ADDRESS=nacos://host.docker.internal:8848?namespace=qiyu-live-test^&username=qiyu^&password=qiyu -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 -e SPRING_DATA_REDIS_HOST=host.docker.internal qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

PowerShell 写法：

```powershell
docker rm qiyu-live-user-provider-01

docker run -d --name qiyu-live-user-provider-01 `
  -p 18085:8085 `
  -p 19090:9090 `
  -e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848 `
  -e DUBBO_REGISTRY_ADDRESS="nacos://host.docker.internal:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu" `
  -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 `
  -e SPRING_DATA_REDIS_HOST=host.docker.internal `
  qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

4. MySQL 地址在 `qiyu-db-sharding.yaml` 中，需要改配置文件后重新构建镜像。

文件位置：

```text
qiyu-live-user-provider/src/main/resources/qiyu-db-sharding.yaml
```

错误写法：

```yaml
jdbcUrl: jdbc:mysql://localhost:3306/qiyu_live_user
jdbcUrl: jdbc:mysql://127.0.0.1:3306/qiyu_live_user
```

容器访问宿主机 MySQL 时应改成：

```yaml
jdbcUrl: jdbc:mysql://host.docker.internal:3306/qiyu_live_user?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

如果配置了从库 `3307`，但本机没有启动 `3307`，可以先临时把读库也指向 `3306`，或者启动真正的 MySQL 从库。

5. 改完配置后必须重新打包、复制 jar、重新构建镜像。

```powershell
mvn -pl qiyu-live-user-provider -am package -DskipTests
copy qiyu-live-user-provider\target\qiyu-live-user-provider-docker.jar qiyu-live-user-provider\docker\
docker build -t qiyu-live-user-provider-docker:1.0-SNAPSHOT qiyu-live-user-provider\docker
```

然后删除旧容器并重新运行：

```powershell
docker rm qiyu-live-user-provider-01
docker run -d --name qiyu-live-user-provider-01 -p 18085:8085 -p 19090:9090 -e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848 -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 -e SPRING_DATA_REDIS_HOST=host.docker.internal qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

6. 如果仍然启动失败，立即查看日志，不要只看 `docker ps`。

```powershell
docker ps -a
docker logs --tail 200 qiyu-live-user-provider-01
```

根据日志中的关键字继续定位：

```text
Nacos      -> 检查 8848、namespace、username、password
MySQL      -> 检查 qiyu-db-sharding.yaml、3306、账号密码、数据库是否存在
Redis      -> 检查 6379、Redis 是否允许连接
RocketMQ   -> 检查 9876 NameServer 和 10911 Broker
Connection refused -> 地址能解析，但目标端口没有服务监听或被拒绝
```

## 十一、如何判断部署成功

GitHub 部署成功的判断：

```powershell
git status
```

显示：

```text
nothing to commit, working tree clean
```

并且 GitHub 页面能看到项目源码。

Docker 镜像构建成功的判断：

```powershell
docker images
```

能看到：

```text
qiyu-live-user-provider-docker   1.0-SNAPSHOT
```

Docker 容器部署成功的判断：

```powershell
docker ps
```

能看到：

```text
qiyu-live-user-provider-01   Up
```

同时端口映射包含：

```text
0.0.0.0:8085->8085/tcp
0.0.0.0:19090->9090/tcp
```

此时说明 `qiyu-live-user-provider` 已经以 Docker 容器方式运行。

## 十二、本次部署踩坑记录与防范

本节记录本次实际部署过程中遇到的问题、原因、解决办法和后续防范方式。

### 1. Git 命令不在项目目录执行

错误现象：

```text
fatal: not a git repository (or any of the parent directories): .git
```

原因：

执行 `git remote add origin ...` 时，命令行所在目录不是项目根目录。

解决办法：

```powershell
cd /d D:\BaiduNetdiskDownload\qiyu-live-app
```

然后再执行 Git 命令。

防范方式：

每次执行 Git 命令前先确认当前目录：

```powershell
cd
git status
```

只要 `git status` 能正常显示分支状态，说明当前目录是 Git 仓库。

### 2. GitHub 远程地址拼写错误

错误写法：

```text
http://githup.com/qxy22/qiyu-live-app.git
```

问题：

`githup.com` 拼写错误，应该是 `github.com`。

正确写法：

```powershell
git remote add origin https://github.com/qxy22/qiyu-live-app.git
```

如果已经添加了错误地址，使用：

```powershell
git remote set-url origin https://github.com/qxy22/qiyu-live-app.git
```

防范方式：

添加远程地址后立刻检查：

```powershell
git remote -v
```

确认输出中是：

```text
https://github.com/qxy22/qiyu-live-app.git
```

### 3. `git add` 没有指定路径

错误命令：

```powershell
git add
```

错误现象：

```text
Nothing specified, nothing added.
hint: Maybe you wanted to say 'git add .'?
```

原因：

`git add` 后面需要指定文件或目录。只写 `git add` 不会添加任何文件。

正确命令：

```powershell
git add .
```

防范方式：

提交前执行：

```powershell
git status --short
```

如果文件前面是 `??` 或红色修改状态，说明还没有加入暂存区。执行 `git add .` 后再检查一次。

### 4. GitHub 页面看不到项目代码

现象：

GitHub 页面只看到 `.gitignore`、`README.md`、许可证等初始文件，看不到项目源码。

原因：

本地代码推送到了 `master` 分支，但 GitHub 页面默认展示的是 `main` 分支。

解决办法：

在 GitHub 页面左上角分支下拉框中切换到：

```text
master
```

防范方式：

推送后观察 GitHub 页面显示的分支名称。如果仓库有 `main` 和 `master` 两个分支，要确认自己正在查看的是代码所在分支。

如果希望默认打开就看到项目代码，可以在 GitHub 仓库中设置：

```text
Settings -> Branches -> Default branch -> master
```

### 5. Docker 基础镜像拉取失败

错误现象：

```text
failed to resolve source metadata for docker.io/library/openjdk:17-jdk-alpine
Head "https://registry.docker-cn.com/..." EOF
```

原因：

Docker 构建镜像时需要先拉取基础镜像。原来的基础镜像是：

```dockerfile
FROM openjdk:17-jdk-alpine
```

拉取过程中使用了失效或不稳定的 Docker 镜像源 `registry.docker-cn.com`，导致下载失败。

解决办法：

将基础镜像改成更常用的 Java 17 运行时镜像：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
```

然后重新拉取：

```powershell
docker pull eclipse-temurin:17-jre-alpine
```

再重新构建：

```powershell
docker build -t qiyu-live-user-provider-docker:1.0-SNAPSHOT qiyu-live-user-provider\docker
```

防范方式：

如果 Docker 拉镜像失败，先单独执行：

```powershell
docker pull 镜像名:标签
```

如果单独拉取也失败，优先检查 Docker Desktop 的镜像源配置和网络代理。

### 6. Docker 宿主机端口被占用

错误现象：

```text
ports are not available
listen tcp 0.0.0.0:8082: bind
Only one usage of each socket address is normally permitted
```

或者：

```text
listen tcp 0.0.0.0:9090: bind
Only one usage of each socket address is normally permitted
```

原因：

宿主机上的 `8082`、`8085` 或 `9090` 已经被其他程序占用，Docker 无法再绑定同一个端口。

解决办法：

查询端口占用：

```powershell
netstat -ano | findstr :8085
netstat -ano | findstr :9090
```

如果不想关闭占用端口的程序，可以换宿主机端口。例如：

```powershell
docker run -d --name qiyu-live-user-provider-01 -p 18085:8085 -p 19090:9090 qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

含义：

```text
宿主机 18085 -> 容器 8085
宿主机 19090 -> 容器 9090
```

防范方式：

启动容器前先检查端口：

```powershell
netstat -ano | findstr :8085
netstat -ano | findstr :19090
```

如果有 `LISTENING`，说明端口已被占用，需要换端口或关闭对应程序。

### 7. 容器名已存在

错误现象：

```text
Conflict. The container name "/qiyu-live-user-provider-01" is already in use
```

原因：

之前执行 `docker run` 虽然启动失败，但 Docker 已经创建了一个失败的容器，占用了容器名。

解决办法：

删除旧容器：

```powershell
docker rm qiyu-live-user-provider-01
```

如果容器还在运行，先停止再删除：

```powershell
docker stop qiyu-live-user-provider-01
docker rm qiyu-live-user-provider-01
```

防范方式：

每次重新运行同名容器前，先查看：

```powershell
docker ps -a
```

确认是否已经存在同名容器。

### 8. 容器里的 `127.0.0.1` 和 `localhost` 用错

错误现象：

Nacos、Redis、RocketMQ、MySQL 在 Windows 本机可以访问，但容器里的项目访问失败。

原因：

容器中的：

```text
127.0.0.1
localhost
```

指的是容器自己，不是 Windows 宿主机。

如果配置写成：

```yaml
server-addr: 127.0.0.1:8848
rocketmq:
  name-server: 127.0.0.1:9876
spring:
  data:
    redis:
      host: localhost
```

容器会在自己内部找 Nacos、RocketMQ、Redis，通常会失败。

解决办法：

Windows Docker Desktop 中，容器访问宿主机可以使用：

```text
host.docker.internal
```

启动容器时通过环境变量覆盖配置：

```powershell
docker run -d --name qiyu-live-user-provider-01 `
  -p 18085:8085 `
  -p 19090:9090 `
  -e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848 `
  -e DUBBO_REGISTRY_ADDRESS="nacos://host.docker.internal:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu" `
  -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 `
  -e SPRING_DATA_REDIS_HOST=host.docker.internal `
  qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

如果在 CMD 中执行，`&` 需要转义成 `^&`：

```cmd
-e DUBBO_REGISTRY_ADDRESS=nacos://host.docker.internal:8848?namespace=qiyu-live-test^&username=qiyu^&password=qiyu
```

防范方式：

只要应用运行在 Docker 容器里，就不要把外部服务地址写成 `127.0.0.1` 或 `localhost`。本机服务用 `host.docker.internal`，Docker 网络中的服务用容器名或服务名。

### 9. Nacos 连接失败

错误现象：

```text
java.lang.IllegalStateException: Failed to create nacos config service client.
Reason: server status check failed.
```

原因：

容器无法访问 Nacos。常见原因是 Nacos 地址写成了 `127.0.0.1:8848`，或者本机 Nacos 没有启动。

解决办法：

先在浏览器确认 Nacos 是否可访问：

```text
http://localhost:8848/nacos
```

如果 Nacos 运行在宿主机，启动容器时覆盖 Nacos 地址：

```cmd
-e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848
```

Dubbo 注册中心也要一起改：

```cmd
-e DUBBO_REGISTRY_ADDRESS=nacos://host.docker.internal:8848?namespace=qiyu-live-test^&username=qiyu^&password=qiyu
```

防范方式：

先确认宿主机 Nacos 能访问，再启动业务容器。启动失败后第一时间查看：

```powershell
docker logs --tail 200 qiyu-live-user-provider-01
```

### 10. RocketMQ 连接失败

错误现象：

```text
org.apache.rocketmq.remoting.exception.RemotingConnectException: connect to null failed
```

原因：

RocketMQ NameServer 地址没有正确传入，或者容器无法访问 `127.0.0.1:9876`。

解决办法：

如果 RocketMQ 运行在 Windows 宿主机，启动容器时添加：

```cmd
-e ROCKETMQ_NAME_SERVER=host.docker.internal:9876
```

同时确认 RocketMQ NameServer 和 Broker 是否启动：

```cmd
netstat -ano | findstr :9876
netstat -ano | findstr :10911
```

防范方式：

不要只启动 RocketMQ NameServer，也要确认 Broker 正常启动。业务容器启动失败时，查看日志中是否出现 RocketMQ 相关异常。

### 11. MySQL 连接失败

错误现象：

```text
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
java.net.ConnectException: Connection refused
HikariPool - Exception during pool initialization
Failed to initialize pool
```

原因：

项目使用 ShardingSphere，数据库地址写在：

```text
qiyu-live-user-provider/src/main/resources/qiyu-db-sharding.yaml
```

本次配置中原来写的是：

```yaml
jdbc:mysql://localhost:3306/qiyu_live_user
jdbc:mysql://localhost:3307/qiyu_live_user
```

容器中的 `localhost` 指向容器自己，所以无法访问宿主机 MySQL。

另外，本机只启动了 `3306`，没有启动 `3307`，所以从库地址也会失败。

解决办法：

先确认 MySQL 是否启动：

```cmd
netstat -ano | findstr :3306
```

如果看到：

```text
0.0.0.0:3306 LISTENING
```

说明 MySQL 正在监听。

然后把 `qiyu-db-sharding.yaml` 中的 JDBC 地址改成：

```yaml
jdbc:mysql://host.docker.internal:3306/qiyu_live_user
```

如果本机没有启动 `3307` 从库，可以先把读库也临时指向 `3306`：

```yaml
jdbc:mysql://host.docker.internal:3306/qiyu_live_user
```

修改后必须重新打包、复制 jar、构建镜像：

```powershell
mvn -pl qiyu-live-user-provider -am package -DskipTests
copy qiyu-live-user-provider\target\qiyu-live-user-provider-docker.jar qiyu-live-user-provider\docker\
docker build -t qiyu-live-user-provider-docker:1.0-SNAPSHOT qiyu-live-user-provider\docker
```

防范方式：

容器化前统一检查所有配置文件中的外部服务地址：

```powershell
Select-String -Path qiyu-live-user-provider\src\main\resources\*.yml,qiyu-live-user-provider\src\main\resources\*.yaml -Pattern "localhost|127.0.0.1|3306|3307|8848|9876"
```

凡是容器需要访问宿主机服务的地方，都要改成 `host.docker.internal` 或通过环境变量覆盖。

### 12. 修改配置后忘记重新构建镜像

现象：

明明已经修改了 `application.yml`、`qiyu-db-sharding.yaml` 或 `Dockerfile`，容器启动后仍然使用旧配置。

原因：

Docker 容器运行的是镜像里的 jar。修改源码文件后，如果没有重新打包和构建镜像，容器里仍然是旧 jar。

正确流程：

```powershell
mvn -pl qiyu-live-user-provider -am package -DskipTests
copy qiyu-live-user-provider\target\qiyu-live-user-provider-docker.jar qiyu-live-user-provider\docker\
docker build -t qiyu-live-user-provider-docker:1.0-SNAPSHOT qiyu-live-user-provider\docker
docker rm qiyu-live-user-provider-01
docker run -d --name qiyu-live-user-provider-01 -p 18085:8085 -p 19090:9090 qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

防范方式：

只要改了 Java 代码、配置文件或 Dockerfile，就按完整流程重新构建镜像。不要只重启旧容器。

### 13. 把 jar 包放进 Docker 目录后可能误提交

现象：

为了构建 Docker 镜像，需要把：

```text
qiyu-live-user-provider-docker.jar
```

复制到：

```text
qiyu-live-user-provider/docker/
```

这个 jar 大约 183 MB，如果误提交到 GitHub，会导致仓库变大。

解决办法：

在 `.gitignore` 中添加：

```gitignore
qiyu-live-user-provider/docker/*.jar
```

防范方式：

每次提交前执行：

```powershell
git status --short
```

确认没有 `.jar`、`target/` 等构建产物出现在待提交列表里。

### 14. 推荐的最终启动命令

本项目在当前本机环境下，推荐使用下面命令启动容器：

CMD 写法：

```cmd
docker rm qiyu-live-user-provider-01

docker run -d --name qiyu-live-user-provider-01 -p 18085:8085 -p 19090:9090 -e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848 -e DUBBO_REGISTRY_ADDRESS=nacos://host.docker.internal:8848?namespace=qiyu-live-test^&username=qiyu^&password=qiyu -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 -e SPRING_DATA_REDIS_HOST=host.docker.internal qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

PowerShell 写法：

```powershell
docker rm qiyu-live-user-provider-01

docker run -d --name qiyu-live-user-provider-01 `
  -p 18085:8085 `
  -p 19090:9090 `
  -e SPRING_CLOUD_NACOS_SERVER_ADDR=host.docker.internal:8848 `
  -e DUBBO_REGISTRY_ADDRESS="nacos://host.docker.internal:8848?namespace=qiyu-live-test&username=qiyu&password=qiyu" `
  -e ROCKETMQ_NAME_SERVER=host.docker.internal:9876 `
  -e SPRING_DATA_REDIS_HOST=host.docker.internal `
  qiyu-live-user-provider-docker:1.0-SNAPSHOT
```

启动后检查：

```powershell
docker ps
docker logs --tail 200 qiyu-live-user-provider-01
```
