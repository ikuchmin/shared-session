package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;

import java.util.List;
import java.util.UUID;

public interface SharedUserPermissionRepository {

    List<String> retrieveAllPermissionsForUser(Id<User, UUID> userId);

    boolean isPermissionGrantedToUser(String permission, Id<User, UUID> userId);

    void grantPermissionToUser(String permission, Id<User, UUID> userId);

    void grantPermissionToUsers(String permission, Ids<User, UUID> userIds);

}
