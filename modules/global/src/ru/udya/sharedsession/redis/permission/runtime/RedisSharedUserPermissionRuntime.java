package ru.udya.sharedsession.redis.permission.runtime;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionBuildHelper;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionParentHelper;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionWildcardHelper;
import ru.udya.sharedsession.permission.repository.SharedUserSessionPermissionRepository;
import ru.udya.sharedsession.permission.runtime.SharedUserSessionPermissionRuntime;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component(SharedUserSessionPermissionRuntime.NAME)
public class RedisSharedUserPermissionRuntime
        implements SharedUserSessionPermissionRuntime {

    protected SharedUserPermissionBuildHelper permissionHelper;
    protected SharedUserPermissionWildcardHelper permissionWildcardHelper;
    protected SharedUserPermissionParentHelper permissionParentHelper;

    protected SharedUserSessionPermissionRepository sessionPermissionRepository;

    @Override
    public boolean isPermissionGrantedToUserSession(SharedUserSession userSession, SharedUserPermission permission) {

        // Redis implementation doesn't support so deep permissions
        if (permission instanceof SharedUserEntityAttributePermission
            || permission instanceof SharedUserScreenElementPermission) {

            return true;
        }

        List<SharedUserPermission> permissions =
                permissionParentHelper.calculateParentPermissions(permission);

        permissions = Stream.concat(Stream.of(permission),
                                    permissions.stream())
                            .distinct().collect(toList());

        for (SharedUserPermission perm : permissions) {
            var isGranted = sessionPermissionRepository
                    .doesHavePermission(userSession, perm);

            if (isGranted) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPermissionsGrantedToUserSession(SharedUserSession userSession,
                                                     List<SharedUserPermission> permission) {
        return false;
    }

    @Override
    public void grantPermissionToUserSession(SharedUserSession userSession, SharedUserPermission permission) {

    }

    @Override
    public void grantPermissionsToUserSession(SharedUserSession userSession,
                                              List<? extends SharedUserPermission> permission) {

    }

    @Override
    public void grantPermissionToUserSessions(List<? extends SharedUserSession> userSession,
                                              SharedUserPermission permission) {

    }

    @Override
    public void grantPermissionsToUserSessions(List<? extends SharedUserSession> userSession,
                                               List<? extends SharedUserPermission> permission) {

    }

    @Override
    public void grantPermissionToAllUserSessions(Id<User, UUID> userId, SharedUserPermission permission) {

    }

    @Override
    public void grantPermissionsToAllUserSessions(Id<User, UUID> userId,
                                                  List<? extends SharedUserPermission> permission) {

    }

    @Override
    public void grantPermissionToAllUsersSessions(Ids<User, UUID> userIds, SharedUserPermission permission) {

    }

    @Override
    public void grantPermissionsToAllUsersSessions(Ids<User, UUID> userIds,
                                                   List<? extends SharedUserPermission> permission) {

    }
}
