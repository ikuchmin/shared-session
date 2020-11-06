package ru.udya.sharedsession.permission.runtime;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.domain.SharedUserSessionId;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface SharedUserSessionPermissionRuntime<S extends SharedUserSessionId<ID>, ID extends Serializable> {

    String NAME = "ss_SharedUserSessionPermissionRuntime";

    boolean isPermissionGrantedToUserSession(S userSession, SharedUserPermission permission);

    boolean isPermissionsGrantedToUserSession(S userSession, List<SharedUserPermission> permission);

    void grantPermissionToUserSession(S userSession, SharedUserPermission permission);

    void grantPermissionsToUserSession(S userSession, List<? extends SharedUserPermission> permission);

    void grantPermissionToUserSessions(List<? extends S> userSession, SharedUserPermission permission);

    void grantPermissionsToUserSessions(List<? extends S> userSession, List<? extends SharedUserPermission> permission);

    void grantPermissionToAllUserSessions(Id<User, UUID> userId, SharedUserPermission permission);

    void grantPermissionsToAllUserSessions(Id<User, UUID> userId, List<? extends SharedUserPermission> permission);

    void grantPermissionToAllUsersSessions(Ids<User, UUID> userIds, SharedUserPermission permission);

    void grantPermissionsToAllUsersSessions(Ids<User, UUID> userIds, List<? extends SharedUserPermission> permission);

    void revokePermissionFromUserSession(S userSession, SharedUserPermission permission);

    void revokePermissionsFromUserSession(S userSession, List<? extends SharedUserPermission> permission);

    void revokePermissionFromUserSessions(List<? extends S> userSession, SharedUserPermission permission);

    void revokePermissionsFromUserSessions(List<? extends S> userSession, List<? extends SharedUserPermission> permission);

    void revokePermissionFromAllUserSessions(Id<User, UUID> userId, SharedUserPermission permission);

    void revokePermissionsFromAllUserSessions(Id<User, UUID> userId, List<? extends SharedUserPermission> permission);

    void revokePermissionFromAllUsersSessions(Ids<User, UUID> userIds, SharedUserPermission permission);

    void revokePermissionsFromAllUsersSessions(Ids<User, UUID> userIds, List<? extends SharedUserPermission> permission);

}
