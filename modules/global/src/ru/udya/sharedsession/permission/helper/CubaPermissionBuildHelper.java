package ru.udya.sharedsession.permission.helper;

import com.haulmont.cuba.security.role.BasicRoleDefinition;
import com.haulmont.cuba.security.role.RoleDefinition;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;

@Component("ss_CubaPermissionBuildHelper")
public class CubaPermissionBuildHelper {

    protected CubaPermissionStringRepresentationHelper stringRepresentationHelper;

    public CubaPermissionBuildHelper(CubaPermissionStringRepresentationHelper stringRepresentationHelper) {
        this.stringRepresentationHelper = stringRepresentationHelper;
    }

    public RoleDefinition buildRoleDefinitionBySharedUserPermissions(List<SharedUserPermission> sharedUserPermissions) {
        var roleDefinitionBuilder = BasicRoleDefinition.builder();

        sharedUserPermissions.stream()
                             .map(stringRepresentationHelper
                                          ::convertSharedUserPermissionToCubaPermission)
                             .forEach(cp -> roleDefinitionBuilder.withPermission(cp.getPermissionType(),
                                                                                 cp.getTarget(),
                                                                                 cp.getValue()));

        return roleDefinitionBuilder.build();
    }
}
