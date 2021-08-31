package ru.udya.sharedsession.redis.codec;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;

import java.nio.ByteBuffer;

@Component("ss_RedisSharedUserSessionIdCodec")
public class RedisSessionIdMappingCodec implements RedisCodec<String, RedisSharedUserSessionId> {

    protected static final Logger log = LoggerFactory.getLogger(RedisSessionIdMappingCodec.class);

    protected RedisSharedUserSessionIdTool redisSharedUserSessionIdTool;

    public RedisSessionIdMappingCodec(RedisSharedUserSessionIdTool redisSharedUserSessionIdTool) {
        this.redisSharedUserSessionIdTool = redisSharedUserSessionIdTool;
    }

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public RedisSharedUserSessionId decodeValue(ByteBuffer buf) {
        var commonSharedId = StringCodec.UTF8.decodeValue(buf);
        return RedisSharedUserSessionId.of(commonSharedId);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(RedisSharedUserSessionId value) {
        return StringCodec.UTF8.encodeValue(value.getSharedId());
    }
}
