package org.qiyu.live.id.generate.interfac;

public interface IdBuilderRpc {

    /**
     * 生成有序 ID。
     *
     * <p>底层通过数据库号段分配 + 服务本地内存自增实现。同一个业务 code
     * 会从 t_id_generate_config 中分配一段 ID 到本地，后续优先在本地发号。</p>
     *
     * @param code 业务编码，对应 t_id_generate_config.id
     * @return 有序 ID
     */
    Long getSeqId(int code);

    /**
     * 生成无序 ID。
     *
     * <p>先生成一个有序 ID，再做扰动处理，使最终结果不呈现严格连续递增。</p>
     *
     * @param code 业务编码，对应 t_id_generate_config.id
     * @return 无序但唯一的 ID
     */
    Long getUnSeqId(int code);

    /**
     * 生成带业务前缀的字符串 ID。
     *
     * <p>如果数据库配置了 id_prefix，则返回 id_prefix + 数字 ID；
     * 如果没有配置前缀，则直接返回数字 ID 字符串。</p>
     *
     * @param code 业务编码，对应 t_id_generate_config.id
     * @return 字符串 ID
     */
    String getSeqStrId(int code);
}
