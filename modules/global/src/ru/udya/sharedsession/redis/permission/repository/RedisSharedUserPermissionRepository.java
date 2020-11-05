package ru.udya.sharedsession.redis.permission.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.ScanArgs;
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
import ru.udya.sharedsession.permission.helper.SharedUserPermissionStringRepresentationHelper;
import ru.udya.sharedsession.permission.repository.SharedUserSessionPermissionRepository;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.permission.codec.RedisSharedUserPermissionCodec;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component(SharedUserSessionPermissionRepository.NAME)
public class RedisSharedUserPermissionRepository
        implements SharedUserSessionPermissionRepository<RedisSharedUserSession> {

    public static final String PERMISSION_SUFFIX = "permissions";

    protected RedisClient redisClient;
    protected RedisSharedUserPermissionCodec userPermissionCodec;
    protected SharedUserPermissionStringRepresentationHelper stringRepresentationHelper;

    protected StatefulRedisConnection<String, SharedUserPermission> asyncConnection;


    public RedisSharedUserPermissionRepository(RedisClient redisClient,
                                               RedisSharedUserPermissionCodec userPermissionCodec,
                                               SharedUserPermissionStringRepresentationHelper stringRepresentationHelper) {
        this.redisClient = redisClient;
        this.userPermissionCodec = userPermissionCodec;
        this.stringRepresentationHelper = stringRepresentationHelper;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.asyncConnection = redisClient.connect(userPermissionCodec);
    }

    @Override
    public List<SharedUserPermission> findAllByUserSession(RedisSharedUserSession userSession) {

        var redisKey = createSharedUserSessionPermissionKey(userSession);

        try {
            var readedPermissions = asyncConnection.async()
                                                   .smembers(redisKey)
                                                   .get();

            return new ArrayList<>(readedPermissions);

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
            RedisSharedUserSession userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserEntityPermission.class);
    }

    @Override
    public List<SharedUserEntityAttributePermission> findAllEntityAttributePermissionsByUserSession(
            RedisSharedUserSession userSession) {

        return internalFindPermissionsByUserSessionAndType
                (userSession, SharedUserEntityAttributePermission.class);
    }

    @Override
    public List<SharedUserSpecificPermission> findAllSpecificPermissionsByUserSession(
            RedisSharedUserSession userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserSpecificPermission.class);
    }

    @Override
    public List<SharedUserScreenPermission> findAllScreenPermissionsByUserSession(
            RedisSharedUserSession userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserScreenPermission.class);
    }

    @Override
    public List<SharedUserScreenElementPermission> findAllScreenElementPermissionsByUserSession(
            RedisSharedUserSession userSession) {

        return internalFindPermissionsByUserSessionAndType(
                userSession, SharedUserScreenElementPermission.class);
    }

    public <T extends SharedUserPermission> List<T> internalFindPermissionsByUserSessionAndType(
            RedisSharedUserSession userSession, Class<T> permissionType) {

        var redisKey = createSharedUserSessionPermissionKey(userSession);

        try {
            String typePrefix = stringRepresentationHelper
                    .defineTypeStringPrefixByPermissionType(permissionType);

            var matches = ScanArgs.Builder.matches(typePrefix + ":*");

            var cursor = asyncConnection.async()
                                        .sscan(redisKey, matches)
                                        .get();


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
    public boolean doesHavePermission(RedisSharedUserSession userSession, SharedUserPermission permission) {

        var redisKey = createSharedUserSessionPermissionKey(userSession);

        try {
            return asyncConnection.async()
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

    @Override
    public List<Boolean> doesHavePermissions(RedisSharedUserSession userSession,
                                             List<? extends SharedUserPermission> permissions) {

        return permissions.stream()
                          .map(p -> doesHavePermission(userSession, p))
                          .collect(Collectors.toList());

        // todo use this when redis 6.2.0 will be released
//        var redisKey = createSharedUserSessionPermissionKey(userSession);
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
    public void addToUserSession(RedisSharedUserSession userSession, SharedUserPermission permission) {
        this.addToUserSession(userSession, Collections.singletonList(permission));
    }

    @Override
    public void addToUserSession(RedisSharedUserSession userSession,
                                 List<? extends SharedUserPermission> permissions) {

        // there is because lettuce can't
        // work properly with empty collections
        if (permissions.isEmpty()) {
            return;
        }

        var redisKey = createSharedUserSessionPermissionKey(userSession);

        try {
            var permissionsArray = permissions.toArray(new SharedUserPermission[0]);

            asyncConnection.async()
                           .sadd(redisKey, permissionsArray)
                           .get();

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

    protected String createSharedUserSessionPermissionKey(SharedUserSession sharedUserSession) {
        return sharedUserSession.getSharedId() + ":" + PERMISSION_SUFFIX;
    }
}
