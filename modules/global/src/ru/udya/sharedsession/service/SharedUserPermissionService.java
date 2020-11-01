package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.Permission;
import com.haulmont.cuba.security.entity.User;

import java.util.UUID;

public interface SharedUserPermissionService {

    String NAME = "ss_PermissionRuntimeService";

    void grantPermissionToUsers(Id<Permission, UUID> permissionId, Ids<User, UUID> userIds);

    void grantPermissionsToUsers(Ids<Permission, UUID> permissionId, Ids<User, UUID> userIds);

    void revokePermissionFromUsers(Id<Permission, UUID> permissionId, Ids<User, UUID> userIds);

    void revokePermissionsFromUsers(Id<Permission, UUID> permissionId, Ids<User, UUID> userIds);
}
