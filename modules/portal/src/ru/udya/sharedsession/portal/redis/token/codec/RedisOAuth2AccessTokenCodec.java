package ru.udya.sharedsession.portal.redis.token.codec;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component("ss_RedisOAuth2AccessTokenCodec")
public class RedisOAuth2AccessTokenCodec implements RedisCodec<String, OAuth2AccessToken> {

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public OAuth2AccessToken decodeValue(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return SerializationUtils.deserialize(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(OAuth2AccessToken value) {
        return ByteBuffer.wrap(SerializationUtils.serialize((DefaultOAuth2AccessToken) value));
    }
}
