package ru.udya.sharedsession.portal.redis.token.codec;

import com.haulmont.cuba.core.sys.serialization.SerializationSupport;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component("ss_RedisOAuth2AuthenticationCodec")
public class RedisOAuth2AuthenticationCodec implements RedisCodec<String, OAuth2Authentication> {

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public OAuth2Authentication decodeValue(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        return (OAuth2Authentication) SerializationSupport.deserialize(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(OAuth2Authentication value) {
        return ByteBuffer.wrap(SerializationSupport.serialize(value));
    }
}
