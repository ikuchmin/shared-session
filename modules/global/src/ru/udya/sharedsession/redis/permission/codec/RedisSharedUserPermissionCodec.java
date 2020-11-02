package ru.udya.sharedsession.redis.permission.codec;

import com.haulmont.cuba.core.sys.serialization.SerializationSupport;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionStringRepresentationHelper;

import java.nio.ByteBuffer;
import java.util.UUID;

@Component("ss_RedisUserPermissionCodec")
public class RedisSharedUserPermissionCodec implements RedisCodec<SharedUserPermission, UUID> {

    public static final String KEP_NAMESPACE = "shared:permission";

    protected SharedUserPermissionStringRepresentationHelper stringRepresentationHelper;

    public RedisSharedUserPermissionCodec(
            SharedUserPermissionStringRepresentationHelper stringRepresentationHelper) {
        this.stringRepresentationHelper = stringRepresentationHelper;
    }

    @Override
    public SharedUserPermission decodeKey(ByteBuffer buf) {
        var redisKey = StringCodec.UTF8.decodeKey(buf);

        var permission = redisKey.replace(KEP_NAMESPACE, "");

        return stringRepresentationHelper.convertStringToPermission(permission);
    }

    @Override
    public UUID decodeValue(ByteBuffer buf) {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        return (UUID) SerializationSupport.deserialize(bytes);
    }

    @Override
    public ByteBuffer encodeKey(SharedUserPermission permission) {
        var redisKey = stringRepresentationHelper
                .convertPermissionToString(permission);

        return StringCodec.UTF8.encodeKey(redisKey);
    }

    @Override
    public ByteBuffer encodeValue(UUID value) {
        return ByteBuffer.wrap(SerializationSupport.serialize(value));
    }
}
