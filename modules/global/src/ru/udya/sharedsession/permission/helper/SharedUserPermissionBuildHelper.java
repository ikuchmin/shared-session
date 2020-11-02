package ru.udya.sharedsession.permission.helper;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import static ru.udya.sharedsession.permission.domain.SharedUserPermission.WILDCARD;

@Component("ss_SharedUserPermissionBuildHelper")
public class SharedUserPermissionBuildHelper {


    public SharedUserPermission buildPermission(PermissionType type, String target, int value) {

        throw new IllegalArgumentException(String.format("Permission type (%s) isn't supported", type));
    }

    public SharedUserPermission buildPermission(PermissionType type, String target) {
        return buildPermission(type, target, 1);
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
}
