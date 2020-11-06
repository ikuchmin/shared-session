package ru.udya.sharedsession.redis.permission.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;
import ru.udya.sharedsession.permission.repository.SharedUserPermissionRepository;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import java.util.List;

@Primary
@Component(SharedUserPermissionRepository.NAME)
public class RedisSharedUserPermissionRepositoryCached implements RedisSharedUserPermissionRepository{

    protected RedisSharedUserPermissionRepositoryImpl redisSharedUserPermissionRepositoryImpl;

    public RedisSharedUserPermissionRepositoryCached(
            RedisSharedUserPermissionRepositoryImpl redisSharedUserPermissionRepositoryImpl) {
        this.redisSharedUserPermissionRepositoryImpl = redisSharedUserPermissionRepositoryImpl;
    }

    public List<SharedUserPermission> findAllByUserSession(
            RedisSharedUserSessionId userSession) {
        return redisSharedUserPermissionRepositoryImpl.findAllByUserSession(userSession);
    }

    public List<SharedUserEntityPermission> findAllEntityPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {
        return redisSharedUserPermissionRepositoryImpl.findAllEntityPermissionsByUserSession(userSession);
    }

    public List<SharedUserEntityAttributePermission> findAllEntityAttributePermissionsByUserSession(
            RedisSharedUserSessionId userSession) {
        return redisSharedUserPermissionRepositoryImpl
                .findAllEntityAttributePermissionsByUserSession(userSession);
    }

    public List<SharedUserSpecificPermission> findAllSpecificPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {
        return redisSharedUserPermissionRepositoryImpl.findAllSpecificPermissionsByUserSession(userSession);
    }

    public List<SharedUserScreenPermission> findAllScreenPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {
        return redisSharedUserPermissionRepositoryImpl.findAllScreenPermissionsByUserSession(userSession);
    }

    public List<SharedUserScreenElementPermission> findAllScreenElementPermissionsByUserSession(
            RedisSharedUserSessionId userSession) {
        return redisSharedUserPermissionRepositoryImpl
                .findAllScreenElementPermissionsByUserSession(userSession);
    }

    public boolean doesHavePermission(RedisSharedUserSessionId userSession,
                                      SharedUserPermission permission) {
        return redisSharedUserPermissionRepositoryImpl.doesHavePermission(userSession, permission);
    }

    public List<Boolean> doesHavePermissions(
            RedisSharedUserSessionId userSession,
            List<? extends SharedUserPermission> permissions) {
        return redisSharedUserPermissionRepositoryImpl.doesHavePermissions(userSession, permissions);
    }

    public void addToUserSession(RedisSharedUserSessionId userSession,
                                 SharedUserPermission permission) {
        redisSharedUserPermissionRepositoryImpl.addToUserSession(userSession, permission);
    }

    public void addToUserSession(RedisSharedUserSessionId userSession,
                                 List<? extends SharedUserPermission> permissions) {
        redisSharedUserPermissionRepositoryImpl.addToUserSession(userSession, permissions);
    }
}
