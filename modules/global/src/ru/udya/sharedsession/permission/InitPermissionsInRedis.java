package ru.udya.sharedsession.permission;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.role.RolesService;
import ru.udya.sharedsession.repository.UserRepositoryService;
import ru.udya.sharedsession.service.SharedUserPermissionService;

import java.util.Collection;

public class InitPermissionsInRedis {

    protected RolesService rolesService;

    protected UserRepositoryService userRepository;

    protected SharedUserPermissionService sharedPermissionService;


    public void initPermissions() {

        Collection<Role> allRoles = rolesService.getAllRoles();

        for (Role role : allRoles) {
            var users = userRepository.findAllHavingRole(Id.of(role));

            var permissions = role.getPermissions();

            sharedPermissionService.grantPermissionsToUsers(Ids.of(permissions),
                                                            Ids.of(users));
        }
    }
}
