package ru.udya.sharedsession.permission.repository;

import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;

import java.io.Serializable;
import java.util.List;

public interface SharedUserSessionPermissionRepository<S extends SharedUserSession<ID>, ID extends Serializable> {

    String NAME = "ss_SharedUserSessionPermissionRepository";

    List<SharedUserPermission> findAllByUserSession(S userSession);

    List<SharedUserEntityPermission> findAllEntityPermissionsByUserSession(S userSession);

    List<SharedUserEntityAttributePermission> findAllEntityAttributePermissionsByUserSession(S userSession);

    List<SharedUserSpecificPermission> findAllSpecificPermissionsByUserSession(S userSession);

    List<SharedUserScreenPermission> findAllScreenPermissionsByUserSession(S userSession);

    List<SharedUserScreenElementPermission> findAllScreenElementPermissionsByUserSession(S userSession);

    boolean doesHavePermission(S userSession, SharedUserPermission permission);

    List<Boolean> doesHavePermissions(S userSession, List<? extends SharedUserPermission> permission);

    void addToUserSession(S userSession, SharedUserPermission permission);

    void addToUserSession(S userSession, List<? extends SharedUserPermission> permission);
}
