package ru.udya.sharedsession.permission.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.UUID;

public interface SharedUserPermissionRepository {

    String NAME = "ss_SharedUserPermissionRepository";

    List<String> retrieveAllPermissionsForUser(Id<User, UUID> userId);

    boolean isUserHasPermission(Id<User, UUID> userId, SharedUserPermission permission);

    void addPermissionToUser(SharedUserPermission permission, Id<User, UUID> userId);

    void addPermissionToUsers(SharedUserPermission permission, Ids<User, UUID> userIds);

}
