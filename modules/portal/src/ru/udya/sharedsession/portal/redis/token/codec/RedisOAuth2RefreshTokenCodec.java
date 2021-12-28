package ru.udya.sharedsession.portal.redis.token.codec;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;

import java.nio.ByteBuffer;

import static org.apache.commons.lang3.SerializationUtils.serialize;

public class RedisOAuth2RefreshTokenCodec implements RedisCodec<String, OAuth2RefreshToken> {

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public OAuth2RefreshToken decodeValue(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        return SerializationUtils.deserialize(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(OAuth2RefreshToken value) {
        byte[] serialize = null;
        if (value instanceof DefaultExpiringOAuth2RefreshToken) {
            serialize = serialize((DefaultExpiringOAuth2RefreshToken) value);
        }
        if (value instanceof DefaultOAuth2RefreshToken) {
            serialize = serialize((DefaultOAuth2RefreshToken) value);
        }
        return serialize == null ? null : ByteBuffer.wrap(serialize);
    }
}
