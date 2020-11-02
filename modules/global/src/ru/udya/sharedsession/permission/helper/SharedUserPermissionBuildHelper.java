package ru.udya.sharedsession.permission.helper;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.security.entity.ScreenComponentPermission;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import static ru.udya.sharedsession.permission.domain.SharedUserPermission.WILDCARD;

@Component("ss_SharedUserPermissionBuildHelper")
public class SharedUserPermissionBuildHelper {

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

    public SharedUserPermission buildPermission(PermissionType type, String cubaTarget) {
        return buildPermission(type, cubaTarget, 1);
    }

    public SharedUserPermission buildPermission(PermissionType type, String cubaTarget, int value) {

        SharedUserPermission builtPermission;
        switch (type) {
            case SCREEN:
                builtPermission = convertCubaScreenToSharedUserPermission(cubaTarget, value);
                break;
            case ENTITY_OP:
                builtPermission = convertCubaEntityOpToSharedUserPermission(cubaTarget, value);
                break;
            case ENTITY_ATTR:
                builtPermission = convertCubaEntityAttributeToSharedUserPermission(cubaTarget, value);
                break;
            case SPECIFIC:
                builtPermission = convertCubaSpecificToSharedUserPermission(cubaTarget, value);
                break;
            case UI:
                builtPermission = convertCubaUIToSharedUserPermission(cubaTarget, value);
                break;
            default:
                throw new IllegalArgumentException(String.format("Permission type (%s) isn't supported", type));
        }

        return builtPermission;
    }

    protected SharedUserPermission convertCubaUIToSharedUserPermission(String cubaTarget, int value) {
        var targetParts = cubaTarget.split(cubaTarget);
        return SharedUserPermission.screenElementPermission(targetParts[0], targetParts[1],
                                                            ScreenComponentPermission.fromId(value)
                                                                                     .name().toLowerCase());
    }

    protected SharedUserPermission convertCubaSpecificToSharedUserPermission(String cubaTarget, int value) {
        return SharedUserPermission.specificPermission(cubaTarget, WILDCARD);
    }

    protected SharedUserPermission convertCubaScreenToSharedUserPermission(String cubaTarget, int value) {
        return SharedUserPermission.screenPermission(cubaTarget, WILDCARD);
    }

    protected SharedUserPermission convertCubaEntityOpToSharedUserPermission(String cubaTarget, int value) {
        var targetParts = cubaTarget.split(cubaTarget);
        return SharedUserPermission.entityPermission(targetParts[0], WILDCARD, targetParts[1]);
    }

    protected SharedUserPermission convertCubaEntityAttributeToSharedUserPermission(String cubaTarget, int value) {
        var targetParts = cubaTarget.split(cubaTarget);
        return SharedUserPermission.entityAttributePermission(targetParts[0], WILDCARD,
                                                              targetParts[1], WILDCARD,
                                                              EntityAttrAccess.fromId(value)
                                                                              .name().toLowerCase());
    }
}
