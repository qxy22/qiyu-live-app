package org.qiyu.live.framework.redis.config;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 自定义Redis JSON序列化器，优化字符串/字符类型的存储
 */
public class IGenericJackson2JsonRedisSerializer extends GenericJackson2JsonRedisSerializer {

    /**
     * 构造方法，使用自定义ObjectMapper初始化父类
     */
    public IGenericJackson2JsonRedisSerializer() {
        super(MapperFactory.newInstance());
    }

    /**
     * 重写序列化方法，针对String/Character类型做特殊处理
     * @param source 待序列化对象
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化异常
     */
    @Override
    public byte[] serialize(Object source) throws SerializationException {
        // 若对象非空，且为String/Character类型，直接转字节存储，不做JSON序列化
        if (source != null && ((source instanceof String) || (source instanceof Character))) {
            return source.toString().getBytes();
        }
        // 其他类型走父类的JSON序列化逻辑
        return super.serialize(source);
    }

    /**
     * 内部工具类，创建Jackson ObjectMapper实例
     * （需补充完整实现，以下为标准生产环境写法）
     */
    private static class MapperFactory {
        public static ObjectMapper newInstance() {
            ObjectMapper mapper = new ObjectMapper();
            // 支持序列化非public类
            mapper.setVisibility(mapper.getVisibilityChecker()
                    .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                    .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                    .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY));
            // 支持序列化带类型信息，反序列化时自动还原类型
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
                    ObjectMapper.DefaultTyping.NON_FINAL);
            return mapper;
        }
    }
}