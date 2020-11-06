package ru.udya.sharedsession.redis.permission.repository;

import ru.udya.sharedsession.permission.repository.SharedUserSessionPermissionRepository;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

public interface RedisSharedUserPermissionRepository
        extends SharedUserSessionPermissionRepository<RedisSharedUserSessionId, String> {

    String PERMISSION_SUFFIX = "permissions";
}
