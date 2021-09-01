package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionStringRepresentationHelper;
import ru.udya.sharedsession.permission.runtime.SharedUserSessionPermissionRuntime;
import ru.udya.sharedsession.repository.SharedUserPermissionStorageItemRepositoryService;

import java.util.List;
import java.util.UUID;

@Service(SharedUserPermissionService.NAME)
public class SharedUserPermissionServiceBean implements SharedUserPermissionService {

    protected Metadata metadata;

    protected SharedUserPermissionStorageItemRepositoryService
            sharedUserPermissionStorageItemRepository;

    protected SharedUserSessionPermissionRuntime sharedUserSessionPermissionRuntime;

    protected SharedUserPermissionStringRepresentationHelper sharedPermissionStringHelper;

    public SharedUserPermissionServiceBean(
            SharedUserPermissionStorageItemRepositoryService sharedUserPermissionStorageItemRepository,
            SharedUserSessionPermissionRuntime sharedUserSessionPermissionRuntime,
            SharedUserPermissionStringRepresentationHelper sharedPermissionStringHelper) {

        this.sharedUserPermissionStorageItemRepository = sharedUserPermissionStorageItemRepository;
        this.sharedUserSessionPermissionRuntime = sharedUserSessionPermissionRuntime;
        this.sharedPermissionStringHelper = sharedPermissionStringHelper;
    }

    @Override
    public void grantPermissionToUser(Id<User, UUID> userId, SharedUserPermission permission) {

        var permissionString = sharedPermissionStringHelper.convertPermissionToString(permission);

        sharedUserPermissionStorageItemRepository.createByUserAndPermission(userId, permissionString);

        sharedUserSessionPermissionRuntime.grantPermissionToAllUserSessions(userId, permission);
    }

    @Override
    public void grantPermissionsToUser(Id<User, UUID> userId, List<SharedUserPermission> permissions) {

        var permissionStrings = sharedPermissionStringHelper.convertPermissionsToStrings(permissions);

        sharedUserPermissionStorageItemRepository.createByUserAndPermissions(userId, permissionStrings);

        sharedUserSessionPermissionRuntime.grantPermissionsToAllUserSessions(userId, permissions);
    }

    @Override
    public void grantPermissionToUsers(Ids<User, UUID> userIds, SharedUserPermission permission) {

        var permissionString = sharedPermissionStringHelper.convertPermissionToString(permission);

        sharedUserPermissionStorageItemRepository.createByUsersAndPermission(userIds, permissionString);

        sharedUserSessionPermissionRuntime.grantPermissionToAllUsersSessions(userIds, permission);
    }

    @Override
    public void grantPermissionsToUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions) {

        var permissionStrings = sharedPermissionStringHelper.convertPermissionsToStrings(permissions);

        sharedUserPermissionStorageItemRepository.createByUsersAndPermissions(userIds, permissionStrings);

        sharedUserSessionPermissionRuntime.grantPermissionsToAllUsersSessions(userIds, permissions);
    }

    @Override
    public void revokePermissionFromUser(Id<User, UUID> userId, SharedUserPermission permission) {

        var permissionString = sharedPermissionStringHelper.convertPermissionToString(permission);

        sharedUserPermissionStorageItemRepository.removeAllByUserAndPermission(userId, permissionString);

        sharedUserSessionPermissionRuntime.revokePermissionFromAllUserSessions(userId, permission);
    }

    @Override
    public void revokePermissionsFromUser(Id<User, UUID> userId, List<SharedUserPermission> permissions) {

        var permissionStrings = sharedPermissionStringHelper.convertPermissionsToStrings(permissions);

        sharedUserPermissionStorageItemRepository.removeAllByUserAndPermissions(userId, permissionStrings);

        sharedUserSessionPermissionRuntime.revokePermissionsFromAllUserSessions(userId, permissions);

    }

    @Override
    public void revokePermissionFromUsers(Ids<User, UUID> userIds, SharedUserPermission permission) {

        var permissionString = sharedPermissionStringHelper.convertPermissionToString(permission);

        sharedUserPermissionStorageItemRepository.removeAllByUsersAndPermission(userIds, permissionString);

        sharedUserSessionPermissionRuntime.revokePermissionFromAllUsersSessions(userIds, permission);
    }

    @Override
    public void revokePermissionsFromUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions) {

        var permissionStrings = sharedPermissionStringHelper.convertPermissionsToStrings(permissions);

        sharedUserPermissionStorageItemRepository.removeAllByUsersAndPermissions(userIds, permissionStrings);

        sharedUserSessionPermissionRuntime.revokePermissionsFromAllUsersSessions(userIds, permissions);
    }
}
