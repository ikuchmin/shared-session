package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.UUID;

public interface SharedUserPermissionService {

    String NAME = "ss_PermissionRuntimeService";

    void grantPermissionToUser(Id<User, UUID> userId, SharedUserPermission permission);

    void grantPermissionsToUser(Id<User, UUID> userId, List<SharedUserPermission> permissions);

    void grantPermissionToUsers(Ids<User, UUID> userIds, SharedUserPermission permission);

    void grantPermissionsToUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions);

    void revokePermissionFromUser(Id<User, UUID> userId, SharedUserPermission permission);

    void revokePermissionsFromUser(Id<User, UUID> userId, List<SharedUserPermission> permissions);

    void revokePermissionFromUsers(Ids<User, UUID> userIds, SharedUserPermission permission);

    void revokePermissionsFromUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions);
}
