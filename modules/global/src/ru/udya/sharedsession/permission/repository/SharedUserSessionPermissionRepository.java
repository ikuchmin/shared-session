package ru.udya.sharedsession.permission.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.UUID;

public interface SharedUserSessionPermissionRepository {

    String NAME = "ss_SharedUserSessionPermissionRepository";

    List<SharedUserPermission> retrieveAllPermissionsForUserSession(SharedUserSession userSession);

    List<SharedUserSession> retrieveAllUserSessionsByUser(Id<User, UUID> userId);

    boolean doesUserSessionHavePermission(SharedUserSession userSession, SharedUserPermission permission);

    List<Boolean> doesUserSessionHavePermissions(SharedUserSession userSession, List<? extends SharedUserPermission> permission);

    void addPermissionToUserSession(SharedUserSession userSession, SharedUserPermission permission);

    void addPermissionsToUserSession(SharedUserSession userSession, List<? extends SharedUserPermission> permission);
}
