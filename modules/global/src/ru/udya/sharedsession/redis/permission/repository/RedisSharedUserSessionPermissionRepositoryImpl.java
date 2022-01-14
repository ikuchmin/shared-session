package ru.udya.sharedsession.redis.permission.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionStringRepresentationHelper;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.permission.codec.RedisSharedUserPermissionCodec;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdExpirationTool;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component(RedisSharedUserSessionPermissionRepositoryImpl.NAME)
public class RedisSharedUserSessionPermissionRepositoryImpl implements RedisSharedUserSessionPermissionRepository {

    public static final String NAME = "ss_RedisSharedUserSessionPermissionRepositoryImpl";

    protected RedisClient redisClient;
    protected RedisSharedUserPermissionCodec userPermissionCodec;
    protected SharedUserPermissionStringRepresentationHelper stringRepresentationHelper;
    protected RedisSharedUserSessionIdTool redisRepositoryTool;
    protected RedisSharedUserSessionIdExpirationTool expirationTool;

    protected StatefulRedisConnection<String, SharedUserPermission> asyncReadConnection;

    public RedisSharedUserSessionPermissionRepositoryImpl(RedisClient redisClient,
                                                          RedisSharedUserPermissionCodec userPermissionCodec,
                                                          SharedUserPermissionStringRepresentationHelper stringRepresentationHelper,
                                                          RedisSharedUserSessionIdTool redisRepositoryTool,
                                                          RedisSharedUserSessionIdExpirationTool expirationTool) {
        this.redisClient = redisClient;
        this.userPermissionCodec = userPermissionCodec;
        this.stringRepresentationHelper = stringRepresentationHelper;
        this.redisRepositoryTool = redisRepositoryTool;
        this.expirationTool = expirationTool;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.asyncReadConnection = redisClient.connect(userPermissionCodec);
    }

    @PreDestroy
    public void close() {
        asyncReadConnection.close();
    }

    @Override
    public List<SharedUserPermission> findAllByUserSession(RedisSharedUserSessionId userSession) {

        var redisKey =  redisRepositoryTool.createSharedUserSessionRedisPermissionKey(userSession);

        try {
            var readPermissions = asyncReadConnection.async()
                                                     .smembers(redisKey)
                                                     .get();

            updatePermissionsKeyExpirationTime(redisKey);

            return new ArrayList<>(readPermissions);

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
    public List<SharedUserEntityPermission> findAllEntityPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserEntityPermission.class);
    }

    @Override
    public List<SharedUserEntityAttributePermission> findAllEntityAttributePermissionsByUserSession(
            RedisSharedUserSessionId userSession) {

        return internalFindPermissionsByUserSessionAndType
                (userSession, SharedUserEntityAttributePermission.class);
    }

    @Override
    public List<SharedUserSpecificPermission> findAllSpecificPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserSpecificPermission.class);
    }

    @Override
    public List<SharedUserScreenPermission> findAllScreenPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserScreenPermission.class);
    }

    @Override
    public List<SharedUserScreenElementPermission> findAllScreenElementPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserScreenElementPermission.class);
    }

    public <T extends SharedUserPermission> List<T> internalFindPermissionsByUserSessionAndType(
            RedisSharedUserSessionId userSession, Class<T> permissionType) {

        var redisKey = redisRepositoryTool.createSharedUserSessionRedisPermissionKey(userSession);

        try {
            String typePrefix = stringRepresentationHelper
                    .defineTypeStringPrefixByPermissionType(permissionType);

            var matches = ScanArgs.Builder.matches(typePrefix + ":*");

            var cursor = asyncReadConnection.async()
                                            .sscan(redisKey, matches)
                                            .get();

            updatePermissionsKeyExpirationTime(redisKey);
            //noinspection unchecked
            return cursor.getValues().stream()
                    .map(p -> (T) p)
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during getting user permission", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during getting user permissions", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }


    @Override
    public boolean doesHavePermission(RedisSharedUserSessionId userSession, SharedUserPermission permission) {

        var redisKey = redisRepositoryTool.createSharedUserSessionRedisPermissionKey(userSession);

        try {
            updatePermissionsKeyExpirationTime(redisKey);

            return asyncReadConnection.async()
                                      .sismember(redisKey, permission)
                                      .get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during checking user has permission", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during checking user has permission", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    private void updatePermissionsKeyExpirationTime(String redisKey) throws ExecutionException, InterruptedException {
        expirationTool.updatePermissionsKeyExpirationTime(asyncReadConnection.async(), redisKey);
    }

    @Override
    public List<Boolean> doesHavePermissions(RedisSharedUserSessionId userSession,
                                             List<? extends SharedUserPermission> permissions) {

        return permissions.stream()
                          .map(p -> doesHavePermission(userSession, p))
                          .collect(Collectors.toList());

        // todo use this when redis 6.2.0 will be released
//        var redisKey = createSharedUserSessionRedisPermissionKey(userSession);
//
//        try {
//            var permissionsArray = permissions.toArray(new SharedUserPermission[0]);
//
//            return asyncReadConnection.async()
//                                      .smismember(redisKey, permissionsArray)
//                                      .get();
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new SharedSessionReadingException("Thread is interrupted by external process during checking user has permissions", e);
//        } catch (ExecutionException e) {
//            throw new SharedSessionReadingException("Exception during checking user has permissions", e);
//        } catch (RedisCommandTimeoutException e) {
//            throw new SharedSessionTimeoutException(e);
//        } catch (RedisException e) {
//            throw new SharedSessionException(e);
//        }
    }

    @Override
    public void addToUserSession(RedisSharedUserSessionId userSession, SharedUserPermission permission) {
        this.addToUserSession(userSession, Collections.singletonList(permission));
    }

    @Override
    public void addToUserSession(RedisSharedUserSessionId userSession,
                                 List<? extends SharedUserPermission> permissions) {

        // there is because lettuce can't
        // work properly with empty collections
        if (permissions.isEmpty()) {
            return;
        }

        var redisKey = redisRepositoryTool.createSharedUserSessionRedisPermissionKey(userSession);

        try {
            var permissionsArray = permissions.toArray(new SharedUserPermission[0]);

            asyncReadConnection.async()
                               .sadd(redisKey, permissionsArray)
                               .get();

            updatePermissionsKeyExpirationTime(redisKey);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during adding permissions to user", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during adding permissions to user", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }

    }

    @Override
    public void removeFromUserSession(RedisSharedUserSessionId userSession, SharedUserPermission permission) {
        this.removeFromUserSession(userSession, Collections.singletonList(permission));
    }

    @Override
    public void removeFromUserSession(RedisSharedUserSessionId userSession,
                                      List<? extends SharedUserPermission> permissions) {

        // there is because lettuce can't
        // work properly with empty collections
        if (permissions.isEmpty()) {
            return;
        }

        var redisKey = redisRepositoryTool.createSharedUserSessionRedisPermissionKey(userSession);

        try {
            var permissionsArray = permissions.toArray(new SharedUserPermission[0]);

            asyncReadConnection.async()
                               .srem(redisKey, permissionsArray)
                               .get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during removing permissions from user session", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during removing permissions from user session", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }

    }
}
