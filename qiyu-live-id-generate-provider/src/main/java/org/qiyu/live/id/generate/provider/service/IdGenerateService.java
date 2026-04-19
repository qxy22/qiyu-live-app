package org.qiyu.live.id.generate.provider.service;

public interface IdGenerateService {

    /**
     * 生成有序 ID。
     *
     * @param code 业务编码，对应 t_id_generate_config.id
     * @return 有序 ID
     */
    Long getSeqId(Integer code);

    /**
     * 生成无序 ID。
     *
     * @param code 业务编码，对应 t_id_generate_config.id
     * @return 无序但唯一的 ID
     */
    Long getUnSeqId(Integer code);

    /**
     * 生成带业务前缀的字符串 ID。
     *
     * @param code 业务编码，对应 t_id_generate_config.id
     * @return 字符串 ID
     */
    String getSeqStrId(Integer code);
}
