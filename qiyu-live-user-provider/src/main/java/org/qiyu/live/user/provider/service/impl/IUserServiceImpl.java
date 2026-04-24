package org.qiyu.live.user.provider.service.impl;

import com.sky.ConvertBeanUtils;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.qiyu.live.user.DTO.UserDTO;
import org.qiyu.live.user.provider.dao.mapper.IUserMapper;
import org.qiyu.live.user.provider.dao.mapper.IUserPhoneMapper;
import org.qiyu.live.user.provider.dao.po.UserPO;
import org.qiyu.live.user.provider.dao.po.UserPhonePO;
import org.qiyu.live.user.provider.mq.CacheDeleteProducer;
import org.qiyu.live.user.provider.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * 核心功能：
 * 1. 单个用户查询（Redis缓存）
 * 2. 批量用户查询（Redis缓存 + 多线程分表查询）
 * 3. 用户更新/删除（延迟双删策略保证缓存一致性）
 */
@Service
public class IUserServiceImpl implements IUserService {

    private static final Logger log = LoggerFactory.getLogger(IUserServiceImpl.class);
    
    /**
     * Redis缓存key前缀
     * 完整key格式：qiyu:user:{userId}
     */
    private static final String USER_CACHE_KEY_PREFIX = "qiyu:user:";
    
    /**
     * RocketMQ主题名称
     * 用于延迟双删的消息队列
     */
    private static final String CACHE_DELETE_TOPIC = "cache-delete-topic";
    
    /**
     * 缓存过期时间（分钟）
     * 30分钟后缓存自动失效，防止数据过旧
     */
    private static final long USER_CACHE_EXPIRE_TIME = 30L;
    
    /**
     * 线程池大小
     * 用于批量查询时的多线程并行处理
     */
    private static final int THREAD_POOL_SIZE = 10;

    @Resource
    private IUserMapper userMapper;

    @Resource
    private IUserPhoneMapper userPhoneMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private CacheDeleteProducer cacheDeleteProducer;
    /**
     * 线程池
     * 用于批量查询时并行执行数据库查询
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /**
     * 根据用户ID查询用户信息（带缓存）
     * 
     * 查询流程：
     * 1. 先查Redis缓存
     * 2. 缓存命中则直接返回
     * 3. 缓存未命中则查询数据库
     * 4. 查询结果写入缓存
     * 
     * @param userId 用户ID
     * @return 用户信息DTO，不存在返回null
     */
    @Override
    public UserDTO getUserById(Long userId) {
        // 参数校验
        if (userId == null) {
            return null;
        }
        
        // 构建缓存key
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        
        // 第一步：查询Redis缓存
        Object cachedUser = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            // 缓存命中，直接返回
            return (UserDTO) cachedUser;
        }
        
        // 第二步：缓存未命中，查询数据库
        UserPO userPo = userMapper.selectById(userId);
        if (userPo == null) {
            // 数据库也不存在，返回null
            return null;
        }
        
        // 第三步：将查询结果写入缓存
        UserDTO userDTO = ConvertBeanUtils.convert(userPo, UserDTO.class);
        fillUserPhone(userDTO);
        redisTemplate.opsForValue().set(cacheKey, userDTO, USER_CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        
        return userDTO;
    }

    @Override
    public UserDTO getUserByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        UserPhonePO userPhonePO = userPhoneMapper.selectByPhone(phone);
        if (userPhonePO == null) {
            return null;
        }
        UserDTO userDTO = getUserById(userPhonePO.getUserId());
        if (userDTO != null) {
            userDTO.setPhone(userPhonePO.getPhone());
        }
        return userDTO;
    }

    /**
     * 批量查询用户信息（高并发优化版）
     * 
     * 优化策略：
     * 1. Redis批量缓存查询 - 减少缓存未命中的用户ID
     * 2. 多线程并行查询 - 不同分表并行查询，提高效率
     * 3. 分表键分组 - 按userId%100分组，减少数据库连接数
     * 4. 缓存回写 - 查询结果写入缓存，下次直接命中
     * 
     * @param userIds 用户ID列表
     * @return 用户信息列表（按传入顺序）
     */
    @Override
    public List<UserDTO> batchGetUserByIds(List<Long> userIds) {
        // 参数校验
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 去重，避免重复查询
        List<Long> distinctUserIds = userIds.stream().distinct().toList();
        
        // 结果Map，线程安全
        Map<Long, UserDTO> resultMap = new ConcurrentHashMap<>();
        
        // 缓存未命中的用户ID列表
        List<Long> cacheMissUserIds = new ArrayList<>();
        
        // 第一步：批量查询Redis缓存
        Map<Long, UserDTO> cachedUserMap = new ConcurrentHashMap<>();
        for (Long userId : distinctUserIds) {
            String cacheKey = USER_CACHE_KEY_PREFIX + userId;
            Object cachedUser = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUser != null) {
                // 缓存命中
                cachedUserMap.put(userId, (UserDTO) cachedUser);
            } else {
                // 缓存未命中，记录下来
                cacheMissUserIds.add(userId);
            }
        }
        
        // 将缓存命中的数据加入结果集
        if (!cachedUserMap.isEmpty()) {
            for (Map.Entry<Long, UserDTO> entry : cachedUserMap.entrySet()) {
                resultMap.put(entry.getKey(), entry.getValue());
            }
        }
        
        // 第二步：多线程并行查询数据库（仅查询缓存未命中的）
        if (!cacheMissUserIds.isEmpty()) {
            // 按分表键分组：userId % 100 = tableIndex
            // 这样同一个分表的查询可以在同一个线程中执行，减少连接数
            Map<Long, List<Long>> tableToUserIds = cacheMissUserIds.stream()
                    .collect(Collectors.groupingBy(userId -> userId % 100));
            
            // 创建异步任务列表
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // 为每个分表创建一个异步查询任务
            for (Map.Entry<Long, List<Long>> entry : tableToUserIds.entrySet()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    List<Long> tableUserIds = entry.getValue();
                    for (Long userId : tableUserIds) {
                        // 查询数据库
                        UserPO userPo = userMapper.selectById(userId);
                        if (userPo != null) {
                            UserDTO userDTO = ConvertBeanUtils.convert(userPo, UserDTO.class);
                            // 加入结果集
                            resultMap.put(userId, userDTO);
                            // 写入缓存
                            String cacheKey = USER_CACHE_KEY_PREFIX + userId;
                            redisTemplate.opsForValue().set(cacheKey, userDTO, USER_CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
                        }
                    }
                }, executorService);
                futures.add(future);
            }
            
            // 等待所有异步任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        
        // 第三步：按原始顺序组装结果
        List<UserDTO> result = new ArrayList<>(distinctUserIds.size());
        for (Long userId : distinctUserIds) {
            result.add(resultMap.get(userId));
        }
        return result;
    }

    /**
     * 更新用户信息（延迟双删策略）
     * 
     * 延迟双删流程：
     * 1. 第一次删除缓存
     * 2. 更新数据库
     * 3. 发送延迟消息到RocketMQ
     * 4. RocketMQ消费者延迟1秒后第二次删除缓存
     * 
     * 为什么需要延迟双删？
     * - 问题场景：更新数据库时，读请求读取旧数据写入缓存
     * - 解决方案：延迟一段时间后再次删除缓存，清除脏数据
     * 
     * @param userDTO 用户信息
     * @return 是否成功
     */
    @Override
    public boolean updateUser(UserDTO userDTO) {
        // 参数校验
        if (userDTO == null || userDTO.getUserId() == null) {
            return false;
        }
        
        Long userId = userDTO.getUserId();
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        
        // 第一步：删除缓存（第一次删除）
        redisTemplate.delete(cacheKey);
        log.info("[延迟双删] 第一次删除缓存, userId={}", userId);
        
        // 第二步：更新数据库
        UserPO userPo = ConvertBeanUtils.convert(userDTO, UserPO.class);
        userMapper.updateById(userPo);
        log.info("[延迟双删] 更新数据库完成, userId={}", userId);
        
        // 第三步：发送延迟删除消息到RocketMQ
        // 消费者会在1秒后第二次删除缓存
        cacheDeleteProducer.sendDelayDeleteMessage(userId);
        
        return true;
    }

    /**
     * 删除用户（延迟双删策略）
     * 
     * @param userId 用户ID
     * @return 是否成功
     */
    @Override
    public boolean deleteUser(Long userId) {
        // 参数校验
        if (userId == null) {
            return false;
        }
        
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        
        // 第一步：删除缓存（第一次删除）
        redisTemplate.delete(cacheKey);
        log.info("[延迟双删] 第一次删除缓存, userId={}", userId);
        
        // 第二步：删除数据库记录
        userMapper.deleteById(userId);
        log.info("[延迟双删] 删除数据库记录完成, userId={}", userId);
        
        // 第三步：发送延迟删除消息到RocketMQ
        cacheDeleteProducer.sendDelayDeleteMessage(userId);
        
        return true;
    }

    /**
     * 创建用户
     * 
     * @param userDTO 用户信息
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUser(UserDTO userDTO) {
        // 参数校验
        if (userDTO == null || userDTO.getUserId() == null) {
            return false;
        }
        
        // 转换并插入数据库
        UserPO userPo = ConvertBeanUtils.convert(userDTO, UserPO.class);
        userMapper.insert(userPo);
        if (userDTO.getPhone() != null && !userDTO.getPhone().isBlank()) {
            UserPhonePO userPhonePO = new UserPhonePO();
            userPhonePO.setUserId(userDTO.getUserId());
            userPhonePO.setPhone(userDTO.getPhone());
            userPhonePO.setStatus(1);
            userPhoneMapper.insert(userPhonePO);
        }
        redisTemplate.delete(USER_CACHE_KEY_PREFIX + userDTO.getUserId());
        
        return true;
    }

    private void fillUserPhone(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUserId() == null) {
            return;
        }
        UserPhonePO userPhonePO = userPhoneMapper.selectByUserId(userDTO.getUserId());
        if (userPhonePO != null) {
            userDTO.setPhone(userPhonePO.getPhone());
        }
    }

}
