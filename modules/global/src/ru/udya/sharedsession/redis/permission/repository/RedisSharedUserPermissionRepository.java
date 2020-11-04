package ru.udya.sharedsession.redis.permission.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;
import ru.udya.sharedsession.permission.repository.SharedUserSessionPermissionRepository;
import ru.udya.sharedsession.redis.permission.codec.RedisSharedUserPermissionCodec;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component(SharedUserSessionPermissionRepository.NAME)
public class RedisSharedUserPermissionRepository
        implements SharedUserSessionPermissionRepository {

    public static final String PERMISSION_SUFFIX = "permissions";

    protected RedisClient redisClient;
    protected RedisSharedUserPermissionCodec userPermissionCodec;

    protected StatefulRedisConnection<String, SharedUserPermission> asyncReadConnection;


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
    public List<SharedUserPermission> findAllByUserSession(SharedUserSession userSession) {
        throw new UnsupportedOperationException("Will be implemented later");
    }

    @Override
    public List<SharedUserEntityPermission> findAllEntityPermissionsByUserSession(
            SharedUserSession userSession) {
        return null;
    }

    @Override
    public List<SharedUserEntityAttributePermission> findAllEntityAttributePermissionsByUserSession(
            SharedUserSession userSession) {
        return null;
    }

    @Override
    public List<SharedUserSpecificPermission> findAllSpecificPermissionsByUserSession(
            SharedUserSession userSession) {
        return null;
    }

    @Override
    public List<SharedUserScreenPermission> findAllScreenPermissionsByUserSession(
            SharedUserSession userSession) {
        return null;
    }

    @Override
    public List<SharedUserScreenElementPermission> findAllScreenElementPermissionsByUserSession(
            SharedUserSession userSession) {
        return null;
    }

    @Override
    public boolean doesHavePermission(SharedUserSession userSession, SharedUserPermission permission) {

        var redisKey = createSharedUserSessionPermissionKey(userSession);

        try {
            return asyncReadConnection.async()
                                      .sismember(redisKey, permission)
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
    public List<Boolean> doesHavePermissions(SharedUserSession userSession,
                                             List<? extends SharedUserPermission> permissions) {

        var redisKey = createSharedUserSessionPermissionKey(userSession);

        try {
            var permissionsArray = permissions.toArray(new SharedUserPermission[0]);

            return asyncReadConnection.async()
                                      .smismember(redisKey, permissionsArray)
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
    public void addToUserSession(SharedUserSession userSession, SharedUserPermission permission) {

        var redisKey = createSharedUserSessionPermissionKey(userSession);

        try {
            asyncReadConnection.async()
                               .sadd(redisKey, permission)
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
    public void addToUserSession(SharedUserSession userSession,
                                 List<? extends SharedUserPermission> permission) {

    }

    protected String createSharedUserSessionPermissionKey(SharedUserSession sharedUserSession) {
        return sharedUserSession.getSharedId() + ":" + PERMISSION_SUFFIX;
    }
}
