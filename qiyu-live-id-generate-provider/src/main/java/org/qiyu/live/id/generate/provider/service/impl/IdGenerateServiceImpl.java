package org.qiyu.live.id.generate.provider.service.impl;

import jakarta.annotation.Resource;
import org.qiyu.live.id.generate.provider.dao.mapper.IdBuilderMapper;
import org.qiyu.live.id.generate.provider.dao.po.IdBuilderPO;
import org.qiyu.live.id.generate.provider.service.IdGenerateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于数据库号段的分布式 ID 生成器。
 *
 * <p>核心思路：</p>
 * <p>1. 数据库表 t_id_generate_config 负责保存每个业务 code 当前可分配的号段。</p>
 * <p>2. 服务实例从数据库抢到一段号后，缓存在本地内存中。</p>
 * <p>3. 本地发号只做 AtomicLong 自增，不需要每次访问数据库。</p>
 * <p>4. 抢号段时通过 version 乐观锁保证多实例不会拿到同一段号。</p>
 */
@Service
public class IdGenerateServiceImpl implements IdGenerateService {

    private static final Logger logger = LoggerFactory.getLogger(IdGenerateServiceImpl.class);

    private static final long UN_SEQ_MULTIPLIER = 2862933555777941757L;
    private static final long UN_SEQ_INCREMENT = 3037000493L;

    /**
     * 多实例同时抢号段时可能因 version 变化而失败，这里限制最大重试次数。
     */
    private static final int MAX_RETRY_TIMES = 10;

    @Resource
    private IdBuilderMapper idBuilderMapper;

    /**
     * 本地号段缓存。
     *
     * <p>key 是业务 code，value 是该业务 code 在当前服务实例内缓存的号段。</p>
     */
    private final ConcurrentMap<Integer, Segment> segmentCache = new ConcurrentHashMap<>();

    @Override
    public Long getSeqId(Integer code) {
        if (code == null) {
            logger.error("id generate code is null");
            return null;
        }
        return nextId(code);
    }

    @Override
    public Long getUnSeqId(Integer code) {
        // 无序 ID 仍然先从数据库号段体系中获取唯一的基础 ID。
        Long id = getSeqId(code);
        if (id == null) {
            return null;
        }
        // 对基础 ID 做扰动，让最终结果不再严格连续。
        return scramble(id);
    }

    @Override
    public String getSeqStrId(Integer code) {
        Long id = getSeqId(code);
        if (id == null) {
            return null;
        }
        // 字符串 ID 需要额外读取业务前缀配置。
        IdBuilderPO idBuilderPO = idBuilderMapper.selectById(code);
        if (idBuilderPO == null || idBuilderPO.getIdPrefix() == null) {
            return String.valueOf(id);
        }
        return idBuilderPO.getIdPrefix() + id;
    }

    /**
     * 从本地号段中获取下一个 ID。
     *
     * <p>如果本地号段已耗尽，则同步加载新的数据库号段后继续发号。</p>
     */
    private Long nextId(Integer code) {
        // 如果当前业务 code 还没有本地号段对象，则先创建一个空号段对象。
        Segment segment = segmentCache.computeIfAbsent(code, key -> new Segment());
        while (true) {
            // 先尝试无锁获取 ID，大多数请求都会走这个分支，性能最高。
            long id = segment.next();
            if (id >= 0) {
                return id;
            }
            // 本地号段耗尽时，只允许同一个业务 code 的一个线程去加载新号段。
            synchronized (segment) {
                // 双重检查：可能其他线程刚刚已经加载好了新号段。
                id = segment.next();
                if (id >= 0) {
                    return id;
                }
                // 当前线程负责从数据库抢下一段号。
                if (!loadNextSegment(code, segment)) {
                    return null;
                }
            }
        }
    }

    /**
     * 从数据库抢占下一个号段，并刷新到当前服务实例的本地缓存。
     */
    private boolean loadNextSegment(Integer code, Segment segment) {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            // 每次重试前都重新读取数据库，拿到最新号段和最新 version。
            IdBuilderPO idBuilderPO = idBuilderMapper.selectById(code);
            if (idBuilderPO == null) {
                logger.error("id builder config is not found, code: {}", code);
                return false;
            }
            if (idBuilderPO.getStep() <= 0) {
                logger.error("id builder step must be positive, code: {}, step: {}", code, idBuilderPO.getStep());
                return false;
            }

            // 当前服务准备拿走 [currentStart, nextThreshold) 这一段。
            long currentStart = idBuilderPO.getCurrentStart();
            long nextThreshold = idBuilderPO.getNextThreshold();
            if (nextThreshold <= currentStart) {
                // 兜底处理：如果数据库号段边界异常，则从 initNum 重新计算一段。
                currentStart = idBuilderPO.getInitNum();
                nextThreshold = currentStart + idBuilderPO.getStep();
            }

            // 数据库需要推进到下一段，避免其他服务实例拿到相同号段。
            long newCurrentStart = nextThreshold;
            long newNextThreshold = nextThreshold + idBuilderPO.getStep();
            // updateCurrentThreshold 的 where 条件包含 version，这是分布式不重复的关键。
            Integer updateResult = idBuilderMapper.updateCurrentThreshold(newNextThreshold, newCurrentStart, idBuilderPO.getId(), idBuilderPO.getVersion());
            if (updateResult != null && updateResult > 0) {
                // 更新成功说明当前实例抢号段成功，可以把旧的数据库号段放入本地缓存。
                segment.reset(currentStart, nextThreshold);
                logger.info("load id segment success, code: {}, start: {}, end: {}", code, currentStart, nextThreshold);
                return true;
            }
        }
        logger.error("load id segment failed after retry, code: {}", code);
        return false;
    }

    /**
     * 将有序 ID 转成非严格连续的 ID。
     *
     * <p>原始 ID 左移 12 位后拼接 12 位随机数，不同原始 ID 的结果区间不会重叠，
     * 因此仍然可以保证唯一性。</p>
     */
    private Long scramble(Long id) {
        return (id * UN_SEQ_MULTIPLIER + UN_SEQ_INCREMENT) & Long.MAX_VALUE;
    }

    /**
     * 当前服务实例内的本地号段。
     *
     * <p>例如 start=100000，end=101000，则可发出的 ID 是 100000 到 100999。</p>
     */
    private static class Segment {

        /**
         * 当前发号位置，使用 AtomicLong 保证单实例内多线程自增不重复。
         */
        private final AtomicLong current = new AtomicLong(0);

        /**
         * 当前号段结束阈值，采用左闭右开区间：[current, threshold)。
         */
        private volatile long threshold = 0;

        private long next() {
            long value = current.getAndIncrement();
            if (value < threshold) {
                return value;
            }
            // 返回 -1 表示本地号段已经耗尽，需要重新加载。
            return -1;
        }

        private void reset(long start, long end) {
            // 加载新号段时，把当前位置重置为新号段起点。
            current.set(start);
            threshold = end;
        }
    }
}
