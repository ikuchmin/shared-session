package ru.udya.sharedsession.redis.codec;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;

import java.nio.ByteBuffer;
import java.util.UUID;

@Component("ss_RedisSharedUserSessionIdCodec")
public class RedisSharedUserSessionIdCodec implements RedisCodec<UUID, RedisSharedUserSessionId> {

    protected RedisSharedUserSessionIdTool redisSharedUserSessionIdTool;

    public RedisSharedUserSessionIdCodec(RedisSharedUserSessionIdTool redisSharedUserSessionIdTool) {
        this.redisSharedUserSessionIdTool = redisSharedUserSessionIdTool;
    }

    @Override
    public UUID decodeKey(ByteBuffer buf) {
        long firstLong = buf.getLong();
        long secondLong = buf.getLong();
        return new UUID(firstLong, secondLong);
    }

    @Override
    public RedisSharedUserSessionId decodeValue(ByteBuffer buf) {
        var commonSharedId = StringCodec.UTF8.decodeValue(buf);
        return RedisSharedUserSessionId.of(commonSharedId);
    }

    @Override
    public ByteBuffer encodeKey(UUID key) {
        ByteBuffer buf = ByteBuffer.wrap(new byte[16]);
        buf.putLong(key.getMostSignificantBits());
        buf.putLong(key.getLeastSignificantBits());
        return buf;
    }

    @Override
    public ByteBuffer encodeValue(RedisSharedUserSessionId value) {
        return StringCodec.UTF8.encodeValue(value.getSharedId());
    }
}
