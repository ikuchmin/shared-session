package ru.udya.sharedsession.repository;


import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.entity.SharedUserPermissionStorageItem;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.UUID;

public interface SharedUserPermissionRepositoryService {

    String NAME = "ss_SharedUserPermissionStorageItemRepositoryService";

    List<SharedUserPermissionStorageItem> findAllByUserId(Id<User, UUID> userId);

    void addPermissionToUser(Id<User, UUID> userId, SharedUserPermission sharedUserPermission);

    void addPermissionsToUser(Id<User, UUID> userId, List<SharedUserPermission> permissions);

    void addPermissionToUsers(Ids<User, UUID> userIds, SharedUserPermission permission);

    void addPermissionsToUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions);

    void removePermissionFromUser(Id<User, UUID> userId, SharedUserPermission permission);

    void removePermissionsFromUser(Id<User, UUID> userId, List<SharedUserPermission> permissions);

    void removePermissionFromUsers(Ids<User, UUID> userIds, SharedUserPermission permission);

    void removePermissionsFromUsers(Ids<User, UUID> userIds, List<SharedUserPermission> permissions);
}
