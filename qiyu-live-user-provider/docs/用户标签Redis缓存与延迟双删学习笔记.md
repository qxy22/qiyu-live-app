# 用户标签 Redis 缓存与延迟双删学习笔记

## 1. 今天学习了什么

今天给用户标签功能引入了 Redis 缓存，并进一步实现了延迟双删，目的是提升用户标签查询性能，同时尽量保证缓存和数据库之间的数据一致性。

用户标签原来的核心逻辑是：

1. `containTag` 每次判断用户是否拥有某个标签时，都会直接查询数据库。
2. `setTag` 通过位运算 `|` 给用户设置标签。
3. `cancelTag` 通过位运算 `& ~` 给用户取消标签。
4. Redis 之前只用于初始化用户标签记录时的短期锁，没有缓存用户标签数据本身。

本次改造后的核心逻辑是：

1. 查询标签时，先查 Redis。
2. Redis 命中时，直接使用缓存中的 `UserTagPO` 做位运算判断。
3. Redis 未命中时，再查数据库，并把数据库结果写回 Redis。
4. 设置或取消标签时，先删除 Redis 标签缓存。
5. 数据库更新成功后，发送 RocketMQ 延迟消息。
6. Consumer 延迟收到消息后，再次删除 Redis 标签缓存。

这就是用户标签功能的 Redis 缓存 + 延迟双删。

## 2. 为什么要引入 Redis 缓存

用户标签判断通常是高频操作，比如：

1. 判断用户是不是 VIP。
2. 判断用户是不是主播。
3. 判断用户是不是高价值用户。
4. 判断用户是不是风控用户。

如果每次判断都查数据库，数据库压力会比较大。

引入 Redis 后，查询链路变成：

```text
containTag
-> 查询 Redis
-> 命中：直接做位运算判断
-> 未命中：查询数据库
-> 数据库有记录：写入 Redis
-> 做位运算判断
```

这样热点用户的标签查询可以直接走 Redis，减少数据库访问。

## 3. Redis 缓存 Key 设计

文件位置：

```text
qiyu-live-user-provider/src/main/java/org/qiyu/live/user/provider/cache/UserProviderCacheKeyBuilder.java
```

新增代码：

```java
private static final String USER_TAG_KEY_PREFIX = "qiyu:user:tag:";

public String buildUserTagKey(Long userId) {
    return USER_TAG_KEY_PREFIX + userId;
}
```

如果用户 ID 是 `10001`，那么 Redis key 是：

```text
qiyu:user:tag:10001
```

这样设计的好处是：

1. key 的含义清晰。
2. 和用户基础信息缓存 `qiyu:user:{userId}` 区分开。
3. 后续如果要单独管理用户标签缓存，也比较方便。

原来已有的初始化锁 key：

```java
private static final String USER_TAG_LOCK_KEY_PREFIX = "qiyu:user:tag:lock:";
```

它用于初始化用户标签记录时加锁，避免并发重复初始化。

## 4. 查询标签时如何使用缓存

文件位置：

```text
qiyu-live-user-provider/src/main/java/org/qiyu/live/user/provider/service/impl/UserTagServiceImpl.java
```

新增缓存过期时间：

```java
private static final long USER_TAG_CACHE_EXPIRE_TIME = 30L;
```

表示用户标签缓存 30 分钟后自动过期。

核心查询方法：

```java
private UserTagPO queryUserTagWithCache(Long userId) {
    String cacheKey = cacheKeyBuilder.buildUserTagKey(userId);
    Object cachedUserTag = redisTemplate.opsForValue().get(cacheKey);
    if (cachedUserTag instanceof UserTagPO userTagPO) {
        return userTagPO;
    }

    UserTagPO userTagPO = userTagMapper.selectById(userId);
    if (userTagPO != null) {
        redisTemplate.opsForValue().set(cacheKey, userTagPO, USER_TAG_CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
    }
    return userTagPO;
}
```

这段代码的执行流程是：

1. 根据 `userId` 构建 Redis key。
2. 使用 `redisTemplate.opsForValue().get(cacheKey)` 查询 Redis。
3. 如果 Redis 中有 `UserTagPO`，直接返回。
4. 如果 Redis 没有数据，就查询数据库。
5. 数据库查到用户标签记录后，写入 Redis，过期时间是 30 分钟。
6. 返回最终的 `UserTagPO`。

原来的 `containTag` 是直接查询数据库：

```java
UserTagPO userTagPO = userTagMapper.selectById(userId);
```

现在改成：

```java
UserTagPO userTagPO = queryUserTagWithCache(userId);
```

这样 `containTag` 就具备了缓存能力。

## 5. 标签判断的位运算没有改变

缓存只是优化数据来源，真正判断用户是否拥有某个标签的逻辑没有变。

代码逻辑：

```java
String queryFieldName = userTagsEnum.getFieldName();
if (UserTagFieldNameConstants.TAG_INFO_01.equals(queryFieldName)) {
    return TagInfoUtils.isContain(userTagPO.getTagInfo01(), userTagsEnum.getTag());
} else if (UserTagFieldNameConstants.TAG_INFO_02.equals(queryFieldName)) {
    return TagInfoUtils.isContain(userTagPO.getTagInfo02(), userTagsEnum.getTag());
} else if (UserTagFieldNameConstants.TAG_INFO_03.equals(queryFieldName)) {
    return TagInfoUtils.isContain(userTagPO.getTagInfo03(), userTagsEnum.getTag());
}
return false;
```

判断方法在：

```text
qiyu-live-user-provider/src/main/java/org/qiyu/live/user/provider/utils/TagInfoUtils.java
```

核心代码：

```java
public static boolean isContain(Long tagInfo, long tag) {
    if (tagInfo == null || tag <= 0) {
        return false;
    }
    return (tagInfo & tag) == tag;
}
```

举例：

```text
tagInfo = 17
二进制 = 10001

tag = 16
二进制 = 10000

tagInfo & tag = 10000
结果等于 tag，说明用户拥有这个标签。
```

所以 Redis 缓存只是减少数据库查询，不改变标签位运算模型。

## 6. 为什么要做延迟双删

如果只在更新数据库前删除一次缓存，可能会出现脏缓存。

问题场景：

```text
T1：线程 A 要设置用户标签，先删除 Redis 缓存
T2：线程 B 查询用户标签，发现 Redis 没有数据
T3：线程 B 查询数据库，此时数据库可能还是旧数据
T4：线程 A 更新数据库成功
T5：线程 B 把刚才查到的旧数据写入 Redis
```

这样 Redis 中就可能保存旧标签数据。

为了解决这个问题，引入延迟双删：

```text
T1：线程 A 先删除 Redis 缓存
T2：线程 A 更新数据库
T3：线程 A 发送 RocketMQ 延迟消息
T4：RocketMQ 延迟 1 秒投递消息
T5：Consumer 收到消息后，再次删除 Redis 缓存
```

第二次删除可以清理并发读请求写入的旧缓存。

## 7. 标签写操作中的第一次删除

文件位置：

```text
qiyu-live-user-provider/src/main/java/org/qiyu/live/user/provider/service/impl/UserTagServiceImpl.java
```

删除标签缓存的方法：

```java
private void evictUserTagCache(Long userId) {
    redisTemplate.delete(cacheKeyBuilder.buildUserTagKey(userId));
}
```

设置标签时：

```java
evictUserTagCache(userId);
boolean updateSuccess = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
if (updateSuccess) {
    cacheDeleteProducer.sendDelayDeleteUserTagMessage(userId);
    return true;
}
```

取消标签时：

```java
evictUserTagCache(userId);
boolean updateSuccess = userTagMapper.cancelTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
if (updateSuccess) {
    cacheDeleteProducer.sendDelayDeleteUserTagMessage(userId);
}
return updateSuccess;
```

这里的关键点是：

1. 先删缓存。
2. 再更新数据库。
3. 数据库更新成功后，发送延迟删除消息。

## 8. RocketMQ 延迟删除消息

文件位置：

```text
qiyu-live-user-provider/src/main/java/org/qiyu/live/user/provider/mq/CacheDeleteProducer.java
```

新增了用户标签缓存消息前缀：

```java
private static final String USER_TAG_CACHE_MESSAGE_PREFIX = "USER_TAG:";
```

发送用户标签延迟删除消息：

```java
public void sendDelayDeleteUserTagMessage(Long userId) {
    sendDelayDeleteMessage(USER_TAG_CACHE_MESSAGE_PREFIX + userId);
    log.info("[RocketMQ] send delay delete user tag cache message, userId={}", userId);
}
```

真正发送消息的方法：

```java
private void sendDelayDeleteMessage(String payload) {
    org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(payload)
            .setHeader(MessageConst.PROPERTY_DELAY_TIME_LEVEL, 1)
            .build();
    rocketMQTemplate.syncSend(TOPIC, message, 3000);
}
```

这里的重点是：

```java
.setHeader(MessageConst.PROPERTY_DELAY_TIME_LEVEL, 1)
```

RocketMQ 延迟级别 `1` 表示延迟 1 秒投递。

标签缓存延迟删除消息的内容类似：

```text
USER_TAG:10001
```

用户基础信息缓存的旧消息仍然是：

```text
10001
```

这样做可以兼容原来的用户基础信息缓存延迟双删逻辑。

## 9. Consumer 如何区分删除哪种缓存

文件位置：

```text
qiyu-live-user-provider/src/main/java/org/qiyu/live/user/provider/mq/CacheDeleteConsumer.java
```

Consumer 收到消息后先判断消息类型：

```java
@Override
public void onMessage(String message) {
    if (message == null || message.isBlank()) {
        log.warn("[RocketMQ] receive empty cache delete message");
        return;
    }

    if (message.startsWith(USER_TAG_CACHE_MESSAGE_PREFIX)) {
        deleteUserTagCache(message);
        return;
    }

    deleteUserCache(message);
}
```

如果消息以 `USER_TAG:` 开头，说明要删除用户标签缓存：

```java
private void deleteUserTagCache(String message) {
    String userIdText = message.substring(USER_TAG_CACHE_MESSAGE_PREFIX.length());
    Long userId = parseUserId(userIdText);
    if (userId == null) {
        return;
    }

    redisTemplate.delete(cacheKeyBuilder.buildUserTagKey(userId));
    log.info("[RocketMQ] delay double delete user tag cache completed, userId={}", userId);
}
```

如果消息不是 `USER_TAG:` 开头，就兼容旧逻辑，删除用户基础信息缓存：

```java
private void deleteUserCache(String message) {
    Long userId = parseUserId(message);
    if (userId == null) {
        return;
    }

    redisTemplate.delete(USER_CACHE_KEY_PREFIX + userId);
    log.info("[RocketMQ] delay double delete user cache completed, userId={}", userId);
}
```

这就是为什么同一个 `cache-delete-topic` 可以同时支持：

1. 用户基础信息缓存删除。
2. 用户标签缓存删除。

## 10. 完整调用链路

### 10.1 查询用户是否有某个标签

```text
UserTagRpcImpl.containTag
-> UserTagServiceImpl.containTag
-> queryUserTagWithCache
-> 先查 Redis：qiyu:user:tag:{userId}
-> 命中：返回 UserTagPO
-> 未命中：查 t_user_tag 分表
-> 查到后写入 Redis，缓存 30 分钟
-> 根据 UserTagsEnum 找到 tag_info_01 / tag_info_02 / tag_info_03
-> TagInfoUtils.isContain 做位运算判断
```

### 10.2 设置用户标签

```text
UserTagRpcImpl.setTag
-> UserTagServiceImpl.setTag
-> evictUserTagCache 第一次删除 Redis 标签缓存
-> userTagMapper.setTag 执行数据库位运算更新
-> 更新成功后发送 RocketMQ 延迟消息 USER_TAG:{userId}
-> 约 1 秒后 CacheDeleteConsumer 收到消息
-> deleteUserTagCache 第二次删除 Redis 标签缓存
```

### 10.3 取消用户标签

```text
UserTagRpcImpl.cancelTag
-> UserTagServiceImpl.cancelTag
-> evictUserTagCache 第一次删除 Redis 标签缓存
-> userTagMapper.cancelTag 执行数据库位运算更新
-> 更新成功后发送 RocketMQ 延迟消息 USER_TAG:{userId}
-> 约 1 秒后 CacheDeleteConsumer 收到消息
-> deleteUserTagCache 第二次删除 Redis 标签缓存
```

## 11. 本次实现涉及的文件

### 11.1 UserProviderCacheKeyBuilder.java

作用：

1. 统一维护 Redis key。
2. 新增用户标签缓存 key。

核心代码：

```java
private static final String USER_TAG_KEY_PREFIX = "qiyu:user:tag:";

public String buildUserTagKey(Long userId) {
    return USER_TAG_KEY_PREFIX + userId;
}
```

### 11.2 UserTagServiceImpl.java

作用：

1. `containTag` 查询 Redis 缓存。
2. Redis 未命中时查数据库并回写缓存。
3. `setTag` / `cancelTag` 中实现第一次删除缓存。
4. 数据库更新成功后发送延迟删除消息。

核心代码：

```java
UserTagPO userTagPO = queryUserTagWithCache(userId);
```

```java
evictUserTagCache(userId);
boolean updateSuccess = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
if (updateSuccess) {
    cacheDeleteProducer.sendDelayDeleteUserTagMessage(userId);
    return true;
}
```

### 11.3 CacheDeleteProducer.java

作用：

1. 发送延迟删除缓存消息。
2. 兼容用户基础信息缓存。
3. 新增用户标签缓存延迟删除消息。

核心代码：

```java
public void sendDelayDeleteUserTagMessage(Long userId) {
    sendDelayDeleteMessage(USER_TAG_CACHE_MESSAGE_PREFIX + userId);
    log.info("[RocketMQ] send delay delete user tag cache message, userId={}", userId);
}
```

### 11.4 CacheDeleteConsumer.java

作用：

1. 消费延迟删除消息。
2. 根据消息格式区分删除用户基础信息缓存还是用户标签缓存。

核心代码：

```java
if (message.startsWith(USER_TAG_CACHE_MESSAGE_PREFIX)) {
    deleteUserTagCache(message);
    return;
}

deleteUserCache(message);
```

## 12. 需要记住的核心思想

### 12.1 缓存查询模式

```text
先查缓存
缓存命中直接返回
缓存未命中查数据库
数据库查到后写回缓存
```

### 12.2 缓存更新模式

```text
先删除缓存
再更新数据库
更新成功后发送延迟消息
延迟后再次删除缓存
```

### 12.3 为什么不是更新缓存

写操作时没有选择直接更新 Redis，而是删除 Redis。

原因是：

1. 删除缓存比更新缓存更简单。
2. 删除后下一次查询会从数据库加载最新数据。
3. 可以避免复杂对象更新时写入错误数据。
4. 配合延迟双删，可以降低并发读写造成脏缓存的概率。

## 13. 复习时可以重点看这些问题

1. Redis key 为什么要设计成 `qiyu:user:tag:{userId}`？
2. `containTag` 为什么要先查 Redis，再查数据库？
3. `setTag` 和 `cancelTag` 为什么要先删除缓存？
4. 为什么数据库更新成功后，还要发送 RocketMQ 延迟消息？
5. Consumer 为什么要根据 `USER_TAG:` 前缀区分消息类型？
6. 用户标签判断为什么使用 `(tagInfo & tag) == tag`？
7. 延迟双删能解决什么并发问题？
8. 延迟双删是否能保证绝对强一致？

第 8 个问题的答案是：延迟双删不能保证绝对强一致，它是一种最终一致性的方案。它能明显降低脏缓存存在的时间和概率，但如果 RocketMQ 不可用、消息发送失败、Consumer 消费失败，仍然可能出现缓存未被第二次删除的问题。因此在更严格的场景下，还需要配合消息重试、死信队列、监控告警或更强的一致性方案。

## 14. 一句话总结

本次改造的本质是：

```text
用 Redis 缓存用户标签查询结果，用 RocketMQ 延迟消息做第二次缓存删除，从而在提升查询性能的同时，尽量保证缓存和数据库最终一致。
```

