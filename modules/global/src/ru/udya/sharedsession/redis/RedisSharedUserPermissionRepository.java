package ru.udya.sharedsession.redis;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import ru.udya.sharedsession.domain.SharedUserPermission;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.redis.codec.RedisSharedUserPermissionCodec;
import ru.udya.sharedsession.repository.SharedUserPermissionRepository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class RedisSharedUserPermissionRepository
        implements SharedUserPermissionRepository {

    public static final String KEY_PREFIX = "shared:permission";

    protected RedisClient redisClient;
    protected RedisSharedUserPermissionCodec userPermissionCodec;

    protected StatefulRedisConnection<SharedUserPermission, UUID> asyncReadConnection;


    public RedisSharedUserPermissionRepository(RedisClient redisClient,
                                               RedisSharedUserPermissionCodec userPermissionCodec) {
        this.redisClient = redisClient;
        this.userPermissionCodec = userPermissionCodec;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.asyncReadConnection = redisClient.connect(userPermissionCodec);
    }

    @Override
    public List<String> retrieveAllPermissionsForUser(Id<User, UUID> userUUIDId) {

        return null;
    }

    @Override
    public boolean isPermissionGrantedToUser(SharedUserPermission permission, Id<User, UUID> userId) {
        var redisKey = buildRedisKeyBySharedUserPermission(permission);

        try {
            return asyncReadConnection.async()
                                      .sismember(redisKey, userId.getValue())
                                      .get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during getting user permission", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during getting user permission", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    @Override
    public void grantPermissionToUser(SharedUserPermission permission, Id<User, UUID> userId) {
        var redisKey = buildRedisKeyBySharedUserPermission(permission);

        try {
            asyncReadConnection.async()
                               .sadd(redisKey, userId.getValue())
                               .get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during granting permission to user", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during granting permission to user", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    @Override
    public void grantPermissionToUsers(SharedUserPermission permission, Ids<User, UUID> userIds) {
        var redisKey = buildRedisKeyBySharedUserPermission(permission);

        var ids = userIds.getValues();

        try {
            asyncReadConnection.async()
                               .sadd(redisKey, ids.toArray(new UUID[0]))
                               .get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during granting permission to user", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during granting permission to user", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    protected String buildRedisKeyBySharedUserPermission(SharedUserPermission permission) {
        return KEY_PREFIX + ":" + permission;
    }
}
