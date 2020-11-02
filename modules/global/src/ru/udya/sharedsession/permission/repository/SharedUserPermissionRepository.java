package ru.udya.sharedsession.permission.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.UUID;

public interface SharedUserPermissionRepository {

    List<String> retrieveAllPermissionsForUser(Id<User, UUID> userId);

    boolean isPermissionGrantedToUser(SharedUserPermission permission, Id<User, UUID> userId);

    void grantPermissionToUser(SharedUserPermission permission, Id<User, UUID> userId);

    void grantPermissionToUsers(SharedUserPermission permission, Ids<User, UUID> userIds);

}
