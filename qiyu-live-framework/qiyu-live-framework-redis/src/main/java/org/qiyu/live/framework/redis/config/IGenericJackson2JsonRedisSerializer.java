package org.qiyu.live.framework.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * Redis serializer with backward-compatible handling for plain string values.
 */
public class IGenericJackson2JsonRedisSerializer extends GenericJackson2JsonRedisSerializer {

    public IGenericJackson2JsonRedisSerializer() {
        super(MapperFactory.newInstance());
    }

    @Override
    public byte[] serialize(Object source) throws SerializationException {
        // Keep string-like values as raw bytes so existing keys stay readable.
        if (source instanceof String || source instanceof Character) {
            return source.toString().getBytes(StandardCharsets.UTF_8);
        }
        return super.serialize(source);
    }

    @Override
    public Object deserialize(byte[] source) throws SerializationException {
        if (source == null || source.length == 0) {
            return null;
        }

        try {
            return super.deserialize(source);
        } catch (SerializationException ex) {
            // Raw string values are not valid JSON payloads, so fall back to UTF-8 text.
            return new String(source, StandardCharsets.UTF_8);
        }
    }

    private static class MapperFactory {
        private static ObjectMapper newInstance() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(mapper.getVisibilityChecker()
                    .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                    .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                    .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY));
            mapper.activateDefaultTyping(
                    mapper.getPolymorphicTypeValidator(),
                    ObjectMapper.DefaultTyping.NON_FINAL
            );
            return mapper;
        }
    }
}
