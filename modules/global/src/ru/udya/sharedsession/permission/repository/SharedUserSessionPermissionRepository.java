package ru.udya.sharedsession.permission.repository;

import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;

import java.util.List;

public interface SharedUserSessionPermissionRepository {

    String NAME = "ss_SharedUserSessionPermissionRepository";

    List<SharedUserPermission> findAllByUserSession(SharedUserSession userSession);

    List<SharedUserEntityPermission> findAllEntityPermissionsByUserSession(SharedUserSession userSession);

    List<SharedUserEntityAttributePermission> findAllEntityAttributePermissionsByUserSession(SharedUserSession userSession);

    List<SharedUserSpecificPermission> findAllSpecificPermissionsByUserSession(SharedUserSession userSession);

    List<SharedUserScreenPermission> findAllScreenPermissionsByUserSession(SharedUserSession userSession);

    List<SharedUserScreenElementPermission> findAllScreenElementPermissionsByUserSession(SharedUserSession userSession);

    boolean doesHavePermission(SharedUserSession userSession, SharedUserPermission permission);

    List<Boolean> doesHavePermissions(SharedUserSession userSession, List<? extends SharedUserPermission> permission);

    void addToUserSession(SharedUserSession userSession, SharedUserPermission permission);

    void addToUserSession(SharedUserSession userSession, List<? extends SharedUserPermission> permission);
}
