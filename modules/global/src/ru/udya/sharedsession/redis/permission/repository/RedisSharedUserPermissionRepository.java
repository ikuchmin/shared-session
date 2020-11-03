package ru.udya.sharedsession.redis.permission.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.repository.SharedUserPermissionRepository;
import ru.udya.sharedsession.redis.permission.codec.RedisSharedUserPermissionCodec;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component(SharedUserPermissionRepository.NAME)
public class RedisSharedUserPermissionRepository
        implements SharedUserPermissionRepository {

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
        throw new UnsupportedOperationException("Will be implemented later");
    }

    @Override
    public boolean isUserHasPermission(Id<User, UUID> userId, SharedUserPermission permission) {

        try {
            return asyncReadConnection.async()
                                      .sismember(permission, userId.getValue())
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
    public void addPermissionToUser(SharedUserPermission permission, Id<User, UUID> userId) {

        try {
            asyncReadConnection.async()
                               .sadd(permission, userId.getValue())
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
    public void addPermissionToUsers(SharedUserPermission permission, Ids<User, UUID> userIds) {

        var ids = userIds.getValues();

        try {
            asyncReadConnection.async()
                               .sadd(permission, ids.toArray(new UUID[0]))
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
}
