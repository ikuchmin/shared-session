package ru.udya.sharedsession.permission.runtime;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.repository.SharedUserSession;

import java.util.List;
import java.util.UUID;

public interface SharedUserSessionPermissionRuntime {

    String NAME = "ss_SharedUserPermissionRuntime";

    boolean isPermissionGrantedToUserSession(SharedUserSession userSession, SharedUserPermission permission);

    boolean isPermissionsGrantedToUserSession(SharedUserSession userSession, List<SharedUserPermission> permission);

    void grantPermissionToUserSession(SharedUserSession userSession, SharedUserPermission permission);

    void grantPermissionsToUserSession(SharedUserSession userSession, List<? extends SharedUserPermission> permission);

    void grantPermissionToUserSessions(List<? extends SharedUserSession> userSession, SharedUserPermission permission);

    void grantPermissionsToUserSessions(List<? extends SharedUserSession> userSession, List<? extends SharedUserPermission> permission);

    void grantPermissionToAllUserSessions(Id<User, UUID> userId, SharedUserPermission permission);

    void grantPermissionsToAllUserSessions(Id<User, UUID> userId, List<? extends SharedUserPermission> permission);

    void grantPermissionToAllUsersSessions(Ids<User, UUID> userIds, SharedUserPermission permission);

    void grantPermissionsToAllUsersSessions(Ids<User, UUID> userIds, List<? extends SharedUserPermission> permission);
}
