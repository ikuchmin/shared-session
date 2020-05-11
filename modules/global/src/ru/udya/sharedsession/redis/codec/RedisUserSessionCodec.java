package ru.udya.sharedsession.redis.codec;

import com.haulmont.cuba.core.sys.serialization.SerializationSupport;
import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;

public class RedisUserSessionCodec implements RedisCodec<String, UserSession> {

    public static final RedisUserSessionCodec INSTANCE = new RedisUserSessionCodec();

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public UserSession decodeValue(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        return (UserSession) SerializationSupport.deserialize(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(UserSession value) {
        return ByteBuffer.wrap(SerializationSupport.serialize(value));
    }
}
