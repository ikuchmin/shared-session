package ru.udya.sharedsession.permission;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.domain.SharedUserPermission;
import ru.udya.sharedsession.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.domain.SharedUserSpecificPermission;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.udya.sharedsession.domain.SharedUserPermission.ALL_ENTITY_ATTRIBUTES_PERMISSIONS;
import static ru.udya.sharedsession.domain.SharedUserPermission.ALL_ENTITY_PERMISSIONS;
import static ru.udya.sharedsession.domain.SharedUserPermission.ALL_SCREEN_PERMISSIONS;
import static ru.udya.sharedsession.domain.SharedUserPermission.ALL_SPECIFIC_PERMISSIONS;
import static ru.udya.sharedsession.domain.SharedUserPermission.WILDCARD;

@Component("ss_PermissionConverter")
public class SharedUserPermissionHelper {

    public static final String PERMISSION_WINDOW_PATTERN = "screen:%s:open"; // screen:screenId:operation
    public static final String PERMISSION_ENTITY_PATTERN = "entity:%s:%s:%s"; // entity:entityType:entityId:operation
    public static final String PERMISSION_ENTITY_ATTRIBUTE_PATTERN = "entity_attribute:%s:%s:%s:%s:%s"; // entity_attribute:entityType:entityId:entityAttribute:entityAttributeValue:operation
    public static final String PERMISSION_SPECIFIC_PATTERN = "specific:%s:perform"; // specific:specificId:operation



    public SharedUserPermission buildPermission(PermissionType type, String target, int value) {

        String typeName;
        switch (type) {
            case SCREEN:
                typeName = "screen";
                break;
            case ENTITY_OP:
                typeName = "entity";
                break;
            case ENTITY_ATTR:
                typeName = "entity_attribute";
                break;
            case SPECIFIC:
                typeName = "specific";
                break;
            case UI:
                typeName = "ui";
                break;
            default:
                throw new IllegalArgumentException(String.format("Permission type (%s) isn't supported", type));
        }

        return typeName + ":" + target;
    }

    public SharedUserPermission buildPermission(PermissionType type, String target) {

        return String.format();
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

    public List<SharedUserPermission> buildWildcardsPermissions(SharedUserPermission permission) {

        if (permission instanceof SharedUserEntityPermission) {
            return buildWildcardsEntityPermissions((SharedUserEntityPermission) permission);
        }

        if (permission instanceof SharedUserEntityAttributePermission) {
            return buildWildcardsEntityAttributePermissions((SharedUserEntityAttributePermission) permission);
        }

        if (permission instanceof SharedUserSpecificPermission) {
            return buildWildcardsSpecificPermissions((SharedUserSpecificPermission) permission);
        }

        if (permission instanceof SharedUserScreenPermission) {
            return buildWildcardsScreenPermissions((SharedUserScreenPermission) permission);
        }

        return Collections.emptyList();
    }

    public List<SharedUserPermission> buildWildcardsEntityPermissions(
            SharedUserEntityPermission permission) {

        return Arrays.asList(
                ALL_ENTITY_PERMISSIONS,

                SharedUserPermission.entityPermission(
                        permission.getEntityType(), WILDCARD, WILDCARD),

                SharedUserPermission.entityPermission(
                        permission.getEntityType(), permission.getEntityId(), WILDCARD),

                SharedUserPermission.entityPermission(
                        permission.getEntityType(), WILDCARD, permission.getOperation()),

                SharedUserPermission.entityPermission(
                        WILDCARD, WILDCARD, permission.getOperation())
                );
    }

    public List<SharedUserPermission> buildWildcardsEntityAttributePermissions(
            SharedUserEntityAttributePermission permission) {

        return Arrays.asList(
                ALL_ENTITY_ATTRIBUTES_PERMISSIONS,

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), WILDCARD,
                        WILDCARD, WILDCARD, WILDCARD),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), permission.getEntityId(),
                        WILDCARD, WILDCARD, WILDCARD),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), WILDCARD,
                        permission.getEntityAttribute(), WILDCARD, WILDCARD),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), WILDCARD,
                        WILDCARD, WILDCARD, permission.getOperation()),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), permission.getEntityId(),
                        permission.getEntityAttribute(), WILDCARD, WILDCARD),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), permission.getEntityId(),
                        WILDCARD, WILDCARD, permission.getOperation()),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), permission.getEntityId(),
                        permission.getEntityAttribute(), permission.getEntityAttributeValue(), WILDCARD),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), permission.getEntityId(),
                        permission.getEntityAttribute(), WILDCARD, permission.getOperation()),

                SharedUserPermission.entityAttributePermission(
                        permission.getEntityType(), permission.getEntityId(),
                        permission.getEntityAttribute(), permission.getEntityAttributeValue(),
                        permission.getOperation()),

                SharedUserPermission.entityAttributePermission(
                        WILDCARD, WILDCARD,
                        WILDCARD, WILDCARD,
                        permission.getOperation())
                );
    }

    public List<SharedUserPermission> buildWildcardsSpecificPermissions(
            SharedUserSpecificPermission permission) {

        return Arrays.asList(
                ALL_SPECIFIC_PERMISSIONS,

                SharedUserPermission.specificPermission(
                        permission.getSpecificPermissionId(), WILDCARD
                ),

                SharedUserPermission.specificPermission(
                        WILDCARD, permission.getOperation()
                )
        );
    }

    public List<SharedUserPermission> buildWildcardsScreenPermissions(
            SharedUserScreenPermission permission) {

        return Arrays.asList(
                ALL_SCREEN_PERMISSIONS,

                SharedUserPermission.screenPermission(
                        permission.getScreenId(), WILDCARD),

                SharedUserPermission.screenPermission(
                        WILDCARD, permission.getOperation()
                )
        );
    }
}
