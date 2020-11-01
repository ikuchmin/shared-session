package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.SharedUserPermissionHelper;
import ru.udya.sharedsession.repository.SharedUserPermissionRepository;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class RedisSharedUserPermissionRuntime
        implements SharedUserPermissionRuntime {

    protected SharedUserPermissionHelper permissionHelper;
    protected SharedUserPermissionRepository sharedUserPermissionRepository;

    @Override
    public boolean isPermissionGrantedToUser(SharedUserPermission permission, Id<User, UUID> userId) {

        List<SharedUserPermission> wildcardsPermissions =
                permissionHelper.buildWildcardsPermissions(permission);

        wildcardsPermissions = wildcardsPermissions.stream()
                                                   .distinct()
                                                   .collect(toList());

        for (SharedUserPermission perm : wildcardsPermissions) {
            var isGranted = sharedUserPermissionRepository
                    .isPermissionGrantedToUser(perm, userId);

            if (isGranted) {
                return true;
            }
        }

        return false;
    }
}
