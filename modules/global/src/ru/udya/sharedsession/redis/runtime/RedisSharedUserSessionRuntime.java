package ru.udya.sharedsession.redis.runtime;

import com.haulmont.cuba.security.global.UserSession;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionBuildHelper;
import ru.udya.sharedsession.redis.cache.RedisSharedUserSessionCache;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserPermissionRepository;
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;
import ru.udya.sharedsession.runtime.SharedUserSessionRuntime;

@Component(SharedUserSessionRuntime.NAME)
public class RedisSharedUserSessionRuntime
        implements SharedUserSessionRuntime {

    protected RedisSharedUserSessionCache sessionCache;

    protected SharedUserPermissionBuildHelper sharedUserPermissionBuildHelper;

    protected RedisSharedUserSessionRepository sharedUserSessionRepository;
    protected RedisSharedUserPermissionRepository sharedUserPermissionRepository;

    public RedisSharedUserSessionRuntime(
            SharedUserPermissionBuildHelper sharedUserPermissionBuildHelper,
            RedisSharedUserSessionRepository sharedUserSessionRepository,
            RedisSharedUserPermissionRepository sharedUserPermissionRepository) {
        this.sharedUserPermissionBuildHelper = sharedUserPermissionBuildHelper;
        this.sharedUserSessionRepository = sharedUserSessionRepository;
        this.sharedUserPermissionRepository = sharedUserPermissionRepository;
    }

    public RedisSharedUserSessionId createByCubaUserSession(UserSession cubaUserSession) {


        var copyOfCubaUserSession = new UserSession(cubaUserSession);

        // clean role definition because it has additional support below
        //noinspection ConstantConditions
        copyOfCubaUserSession.setJoinedRole(null);

        var createdSharedUserSession = sharedUserSessionRepository
                .createByCubaUserSession(cubaUserSession);


        var sharedUserPermissions = sharedUserPermissionBuildHelper
                .buildPermissionsByCubaRoleDefinition(cubaUserSession.getJoinedRole());

        sharedUserPermissionRepository
                .addToUserSession(createdSharedUserSession, sharedUserPermissions);

        return createdSharedUserSession;
    }
}
