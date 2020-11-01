package ru.udya.sharedsession.redis.codec;

import com.haulmont.cuba.core.sys.serialization.SerializationSupport;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.UUID;

@Component("ss_RedisUserPermissionCodec")
public class RedisUserPermissionCodec implements RedisCodec<String, UUID> {

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public UUID decodeValue(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        return (UUID) SerializationSupport.deserialize(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(UUID value) {
        return ByteBuffer.wrap(SerializationSupport.serialize(value));
    }
}
