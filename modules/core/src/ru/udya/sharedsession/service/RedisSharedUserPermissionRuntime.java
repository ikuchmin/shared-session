package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.permission.PermissionHelper;
import ru.udya.sharedsession.repository.SharedUserPermissionRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RedisSharedUserPermissionRuntime
        implements SharedUserPermissionRuntime {

    protected PermissionHelper permissionConverter;
    protected SharedUserPermissionRepository sharedUserPermissionRepository;

    @Override
    public boolean isPermissionGrantedToUser(String permission, Id<User, UUID> userId) {

        List<String> permissionsWithWildcards =
                permissionConverter.buildWildcardsPermissionsBasedOn(permission);

        for (String perm : permissionsWithWildcards) {
            var isGranted = sharedUserPermissionRepository
                    .isPermissionGrantedToUser(perm, userId);

            if (isGranted) {
                return true;
            }
        }

        return false;
    }

    protected List<String> generateWildcardsPermissions(String permission) {


        return Collections.emptyList();
    }
}
