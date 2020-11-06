package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.runtime.SharedUserSessionPermissionRuntime;
import ru.udya.sharedsession.repository.SharedUserPermissionRepositoryService;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Component(SharedUserPermissionService.NAME)
public class SharedUserPermissionServiceBean implements SharedUserPermissionService {

    protected SharedUserPermissionRepositoryService sharedUserPermissionRepository;

    protected SharedUserSessionPermissionRuntime<SharedUserSession<Serializable>, Serializable> sharedUserSessionPermissionRuntime;

    public SharedUserPermissionServiceBean(
            SharedUserPermissionRepositoryService sharedUserPermissionRepository,
            SharedUserSessionPermissionRuntime<SharedUserSession<Serializable>, Serializable> sharedUserSessionPermissionRuntime) {
        this.sharedUserPermissionRepository = sharedUserPermissionRepository;
        this.sharedUserSessionPermissionRuntime = sharedUserSessionPermissionRuntime;
    }

    @Override
    public void grantPermissionToUser(Id<User, UUID> userId, SharedUserPermission permission) {
        sharedUserPermissionRepository.addPermissionToUser(userId, permission);

        sharedUserSessionPermissionRuntime.grantPermissionToAllUserSessions(userId, permission);
    }

    @Override
    public void grantPermissionsToUser(Id<User, UUID> userId, List<SharedUserPermission> permissions) {
        sharedUserPermissionRepository.addPermissionsToUser(userId, permissions);

        sharedUserSessionPermissionRuntime.grantPermissionsToAllUserSessions(userId, permissions);
    }

    @Override
    public void grantPermissionToUsers(Ids<User, UUID> userIds, SharedUserPermission permission) {

        sharedUserPermissionRepository.addPermissionToUsers(userIds, permission);

        sharedUserSessionPermissionRuntime.grantPermissionToAllUsersSessions(userIds, permission);

    }

    @Override
    public void grantPermissionsToUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions) {

        sharedUserPermissionRepository.addPermissionsToUsers(userIds, permissions);

        sharedUserSessionPermissionRuntime.grantPermissionsToAllUsersSessions(userIds, permissions);
    }

    @Override
    public void revokePermissionFromUser(Id<User, UUID> userId, SharedUserPermission permission) {
        sharedUserPermissionRepository.removePermissionFromUser(userId, permission);

        sharedUserSessionPermissionRuntime.revokePermissionFromAllUserSessions(userId, permission);
    }

    @Override
    public void revokePermissionsFromUser(Id<User, UUID> userId, List<SharedUserPermission> permissions) {
        sharedUserPermissionRepository.removePermissionsFromUser(userId, permissions);

        sharedUserSessionPermissionRuntime.revokePermissionsFromAllUserSessions(userId, permissions);

    }

    @Override
    public void revokePermissionFromUsers(Ids<User, UUID> userIds, SharedUserPermission permission) {
        sharedUserPermissionRepository.removePermissionFromUsers(userIds, permission);

        sharedUserSessionPermissionRuntime.revokePermissionFromAllUsersSessions(userIds, permission);
    }

    @Override
    public void revokePermissionsFromUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions) {
        sharedUserPermissionRepository.removePermissionsFromUsers(userIds, permissions);

        sharedUserSessionPermissionRuntime.revokePermissionsFromAllUsersSessions(userIds, permissions);
    }
}
