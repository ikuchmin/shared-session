package ru.udya.sharedsession.redis.permission.repository;

import ru.udya.sharedsession.permission.repository.SharedUserPermissionRepository;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

public interface RedisSharedUserPermissionRepository
        extends SharedUserPermissionRepository<RedisSharedUserSessionId, String> {

    String PERMISSION_SUFFIX = "permissions";
}
