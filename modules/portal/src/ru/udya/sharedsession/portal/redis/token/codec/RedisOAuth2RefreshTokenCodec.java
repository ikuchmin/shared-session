package ru.udya.sharedsession.portal.redis.token.codec;

import com.haulmont.cuba.core.sys.serialization.SerializationSupport;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;

import java.nio.ByteBuffer;

public class RedisOAuth2RefreshTokenCodec implements RedisCodec<String, OAuth2RefreshToken> {

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public OAuth2RefreshToken decodeValue(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        return (OAuth2RefreshToken) SerializationSupport.deserialize(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(OAuth2RefreshToken value) {
        return ByteBuffer.wrap(SerializationSupport.serialize(value));
    }
}
