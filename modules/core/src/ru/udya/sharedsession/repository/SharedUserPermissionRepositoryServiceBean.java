package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.entity.SharedUserPermissionStorageItem;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.UUID;

public class SharedUserPermissionRepositoryServiceBean implements SharedUserPermissionRepositoryService{


    @Override
    public List<SharedUserPermissionStorageItem> findAllByUserId(Id<User, UUID> userId) {

        return null;
    }

    @Override
    public void addPermissionToUser(Id<User, UUID> userId, SharedUserPermission sharedUserPermission) {

    }

    @Override
    public void addPermissionsToUser(Id<User, UUID> userId, List<SharedUserPermission> permissions) {

    }

    @Override
    public void addPermissionToUsers(Ids<User, UUID> userIds, SharedUserPermission permission) {

    }

    @Override
    public void addPermissionsToUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions) {

    }

    @Override
    public void removePermissionFromUser(Id<User, UUID> userId, SharedUserPermission permission) {

    }

    @Override
    public void removePermissionsFromUser(Id<User, UUID> userId, List<SharedUserPermission> permissions) {

    }

    @Override
    public void removePermissionFromUsers(Ids<User, UUID> userIds, SharedUserPermission permission) {

    }

    @Override
    public void removePermissionsFromUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions) {

    }
}
