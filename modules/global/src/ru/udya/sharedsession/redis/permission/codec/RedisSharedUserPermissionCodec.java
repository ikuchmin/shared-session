package ru.udya.sharedsession.redis.permission.codec;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionStringRepresentationHelper;

import java.nio.ByteBuffer;

@Component("ss_RedisUserPermissionCodec")
public class RedisSharedUserPermissionCodec implements RedisCodec<String, SharedUserPermission> {

    protected SharedUserPermissionStringRepresentationHelper stringRepresentationHelper;

    public RedisSharedUserPermissionCodec(
            SharedUserPermissionStringRepresentationHelper stringRepresentationHelper) {
        this.stringRepresentationHelper = stringRepresentationHelper;
    }

    @Override
    public String decodeKey(ByteBuffer buf) {
        return StringCodec.UTF8.decodeKey(buf);
    }

    @Override
    public SharedUserPermission decodeValue(ByteBuffer buf) {
        var permission = StringCodec.UTF8.decodeKey(buf);

        return stringRepresentationHelper
                .convertStringToPermission(permission);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StringCodec.UTF8.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(SharedUserPermission permission) {
        var redisKey = stringRepresentationHelper
                .convertPermissionToString(permission);

        return StringCodec.UTF8.encodeKey(redisKey);    }
}
