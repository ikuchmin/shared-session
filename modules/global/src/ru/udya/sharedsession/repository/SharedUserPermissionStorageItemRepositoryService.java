package ru.udya.sharedsession.repository;


import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.entity.SharedUserPermissionStorageItem;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnusedReturnValue")
public interface SharedUserPermissionStorageItemRepositoryService {

    String NAME = "ss_SharedUserPermissionStorageItemRepositoryService";

    List<SharedUserPermissionStorageItem> findAllByUserId(Id<User, UUID> userId, View view);

    SharedUserPermissionStorageItem createByUserAndPermission(Id<User, UUID> userId, String permission);

    List<SharedUserPermissionStorageItem> createByUserAndPermissions(Id<User, UUID> userId, List<String> permissions);

    List<SharedUserPermissionStorageItem> createByUsersAndPermission(Ids<User, UUID> userIds, String permission);

    List<SharedUserPermissionStorageItem> createByUsersAndPermissions(Ids<User, UUID> userIds, List<String> permissions);

    void removeAllByUserAndPermission(Id<User, UUID> userId, String permission);

    void removeAllByUserAndPermissions(Id<User, UUID> userId, List<String> permissions);

    void removeAllByUsersAndPermission(Ids<User, UUID> userIds, String permission);

    void removeAllByUsersAndPermissions(Ids<User, UUID> userIds, List<String> permissions);
}
