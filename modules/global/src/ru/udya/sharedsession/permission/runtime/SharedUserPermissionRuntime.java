package ru.udya.sharedsession.permission.runtime;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.domain.SharedUserSessionId;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface SharedUserPermissionRuntime<S extends SharedUserSessionId<ID>, ID extends Serializable> {

    String NAME = "ss_SharedUserPermissionRuntime";

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
}
