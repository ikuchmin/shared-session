package ru.udya.sharedsession.redis.runtime;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.entity.SharedUserPermissionStorageItem;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionBuildHelper;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserSessionPermissionRepository;
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;
import ru.udya.sharedsession.repository.SharedUserPermissionStorageItemRepositoryService;
import ru.udya.sharedsession.runtime.SharedUserSessionRuntime;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(SharedUserSessionRuntime.NAME)
public class RedisSharedUserSessionRuntime
        implements SharedUserSessionRuntime {

    protected SharedUserPermissionBuildHelper sharedUserPermissionBuildHelper;

    protected RedisSharedUserSessionRepository sharedUserSessionRepository;
    protected RedisSharedUserSessionPermissionRepository sharedUserPermissionRepository;

    protected SharedUserPermissionStorageItemRepositoryService sharedUserPermissionStorageItemRepository;

    public RedisSharedUserSessionRuntime(SharedUserPermissionBuildHelper sharedUserPermissionBuildHelper,
                                         RedisSharedUserSessionRepository sharedUserSessionRepository,
                                         RedisSharedUserSessionPermissionRepository sharedUserPermissionRepository,
                                         SharedUserPermissionStorageItemRepositoryService sharedUserPermissionStorageItemRepository) {
        this.sharedUserPermissionBuildHelper = sharedUserPermissionBuildHelper;
        this.sharedUserSessionRepository = sharedUserSessionRepository;
        this.sharedUserPermissionRepository = sharedUserPermissionRepository;
        this.sharedUserPermissionStorageItemRepository = sharedUserPermissionStorageItemRepository;
    }

    public RedisSharedUserSessionId createByCubaUserSession(UserSession cubaUserSession) {


        var copyOfCubaUserSession = new UserSession(cubaUserSession);

        // clean role definition because it has additional support below
        //noinspection ConstantConditions
        copyOfCubaUserSession.setJoinedRole(null);

        // create main part of shared session
        var createdSharedUserSession = sharedUserSessionRepository
                .createByCubaUserSession(cubaUserSession);

        // create permission part of shared session
        // build permissions from cuba user session
        var cubaSharedUserPermissions = sharedUserPermissionBuildHelper
                .buildPermissionsByCubaRoleDefinition(cubaUserSession.getJoinedRole());

        // load shared permissions
        var viewWithPermission = ViewBuilder
                .of(SharedUserPermissionStorageItem.class)
                .add("permission").build();

        var loadedSharedUserPermissionStorageItems = sharedUserPermissionStorageItemRepository
                .findAllByUserId(Id.of(cubaUserSession.getUser()), viewWithPermission);

        var loadedSharedUserPermissions = loadedSharedUserPermissionStorageItems.stream()
                .map(sharedUserPermissionBuildHelper::
                        buildPermissionBySharedUserPermissionStorageItem);

        var allSharedUserPermissions = Stream
                .concat(cubaSharedUserPermissions.stream(), loadedSharedUserPermissions)
                .collect(Collectors.toList());

        sharedUserPermissionRepository
                .addToUserSession(createdSharedUserSession, allSharedUserPermissions);

        return createdSharedUserSession;
    }
}
