package org.qiyu.live.id.generate.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.qiyu.live.id.generate.interfac.IdBuilderRpc;
import org.qiyu.live.id.generate.provider.service.IdGenerateService;

/**
 * ID 生成 Dubbo 服务实现。
 *
 * <p>这一层只负责把远程 RPC 请求转发给内部业务 service，
 * 真正的号段加载、缓存和发号逻辑都在 IdGenerateService 中。</p>
 */
@DubboService
public class IdBuilderRpcImpl implements IdBuilderRpc {

    /**
     * 注入 provider 内部的 ID 生成服务，避免 RPC 层直接编写业务逻辑。
     */
    @Resource
    private IdGenerateService idGenerateService;

    @Override
    public Long getSeqId(int code) {
        // 生成有序 ID。
        return idGenerateService.getSeqId(code);
    }

    @Override
    public Long getUnSeqId(int code) {
        // 生成无序 ID。
        return idGenerateService.getUnSeqId(code);
    }

    @Override
    public String getSeqStrId(int code) {
        // 生成带业务前缀的字符串 ID。
        return idGenerateService.getSeqStrId(code);
    }
}
