package ru.udya.sharedsession.permission.helper;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.security.role.RoleDefinition;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.entity.SharedUserPermissionStorageItem;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.udya.sharedsession.permission.domain.SharedUserPermission.WILDCARD;

@Component("ss_SharedUserPermissionBuildHelper")
public class SharedUserPermissionBuildHelper {

    protected SharedUserPermissionStringRepresentationHelper sharedPermissionStringRepresentationHelper;
    protected CubaPermissionStringRepresentationHelper cubaPermissionStringRepresentationHelper;

    public SharedUserPermissionBuildHelper(SharedUserPermissionStringRepresentationHelper sharedPermissionStringRepresentationHelper,
                                           CubaPermissionStringRepresentationHelper cubaPermissionStringRepresentationHelper) {
        this.sharedPermissionStringRepresentationHelper = sharedPermissionStringRepresentationHelper;
        this.cubaPermissionStringRepresentationHelper = cubaPermissionStringRepresentationHelper;
    }

    public SharedUserPermission buildPermissionByWindowAlias(String windowAlias) {

        return SharedUserPermission.screenPermission(windowAlias, WILDCARD);
    }

    public SharedUserPermission buildPermissionByEntity(MetaClass metaClass, EntityOp entityOp) {

        return SharedUserPermission.entityPermission(metaClass.getName(),
                                                     WILDCARD, entityOp.getId());
    }

    public SharedUserPermission buildPermissionByEntity(MetaClass metaClass, String operation) {

        return SharedUserPermission.entityPermission(metaClass.getName(), WILDCARD, operation);
    }

    public SharedUserPermission buildPermissionByEntityAttribute(
            MetaClass metaClass, String property, EntityAttrAccess access) {

        return SharedUserPermission.entityAttributePermission(metaClass.getName(), WILDCARD,
                                                              property, WILDCARD,
                                                              access.name().toLowerCase());
    }

    public SharedUserPermission buildPermissionByEntityAttribute(MetaClass metaClass,
                                                                 String property,
                                                                 String access) {

        return SharedUserPermission.entityAttributePermission(metaClass.getName(), WILDCARD,
                                                              property, WILDCARD, access);

    }

    public SharedUserPermission buildPermissionBySpecificPermission(String name) {
        return SharedUserPermission.specificPermission(name, WILDCARD);
    }

    public SharedUserPermission buildPermissionByCubaTarget(PermissionType type, String cubaTarget) {
        return buildPermissionByCubaTarget(type, cubaTarget, 1);
    }

    public SharedUserPermission buildPermissionByCubaTarget(PermissionType type, String cubaTarget, int value) {

        return cubaPermissionStringRepresentationHelper
                .convertCubaPermissionToSharedUserPermission(type, cubaTarget, value);
    }

    public List<SharedUserPermission> buildPermissionsByCubaRoleDefinition(RoleDefinition roleDefinition) {

        var cubaEntityAllowPermissions = roleDefinition.entityPermissions()
                                                       .getExplicitPermissions()
                                                       .entrySet().stream()
                                                       .filter(kv -> kv.getValue() > 0);

        var sharedUserEntityPermissions = cubaEntityAllowPermissions
                .map(p -> cubaPermissionStringRepresentationHelper
                        .convertCubaEntityPermissionToSharedUserPermission(p.getKey(), p.getValue()));

        var cubaEntityAttributePermissions = roleDefinition.entityAttributePermissions()
                                                           .getExplicitPermissions()
                                                           .entrySet().stream();

        var sharedUserEntityAttributePermissions = cubaEntityAttributePermissions
                .map(p -> cubaPermissionStringRepresentationHelper
                        .convertCubaEntityAttributePermissionToSharedUserPermission(p.getKey(), p.getValue()));


        var cubaSpecificAllowPermissions = roleDefinition.specificPermissions()
                                                         .getExplicitPermissions()
                                                         .entrySet().stream()
                                                         .filter(kv -> kv.getValue() > 0);

        var sharedUserSpecificPermissions = cubaSpecificAllowPermissions
                .map(p -> cubaPermissionStringRepresentationHelper
                        .convertCubaSpecificSpecificToSharedUserPermission(p.getKey(), p.getValue()));


        var cubaScreenAllowPermissions = roleDefinition.screenPermissions()
                                                       .getExplicitPermissions()
                                                       .entrySet().stream()
                                                       .filter(kv -> kv.getValue() > 0);

        var sharedUserScreenPermissions = cubaScreenAllowPermissions
                .map(p -> cubaPermissionStringRepresentationHelper
                        .convertCubaScreenPermissionToSharedUserPermission(p.getKey(), p.getValue()));


        var cubaScreenElementPermissions = roleDefinition.screenComponentPermissions()
                                                         .getExplicitPermissions()
                                                         .entrySet().stream();

        var sharedUserScreenElementPermissions = cubaScreenElementPermissions
                .map(p -> cubaPermissionStringRepresentationHelper
                        .convertCubaUIPermissionToSharedUserPermission(p.getKey(), p.getValue()));

        // manual cast resolve compilation problem don't delete it
        //noinspection RedundantCast
        return Stream.of(sharedUserEntityPermissions,
                         sharedUserEntityAttributePermissions,
                         sharedUserSpecificPermissions,
                         sharedUserScreenPermissions,
                         sharedUserScreenElementPermissions)
                     .flatMap(Function.identity())
                     .map(p -> (SharedUserPermission) p)
                     .collect(Collectors.toList());
    }

    public SharedUserPermission buildPermissionBySharedUserPermissionStorageItem(SharedUserPermissionStorageItem storageItem) {

        return sharedPermissionStringRepresentationHelper
                .convertStringToPermission(storageItem.getPermission());
    }
}
