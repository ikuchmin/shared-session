package ru.udya.sharedsession.permission;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("ss_PermissionConverter")
public class PermissionHelper {

    public static final String PERMISSION_WINDOW_PATTERN = "screen:%s:open"; // screen:screenId:operation
    public static final String PERMISSION_ENTITY_PATTERN = "entity:%s:%s:%s"; // entity:entityType:entityId:operation
    public static final String PERMISSION_ENTITY_ATTRIBUTE_PATTERN = "entity_attribute:%s:%s:%s:%s:%s"; // entity_attribute:entityType:entityId:entityAttribute:entityAttributeValue:operation
    public static final String PERMISSION_SPECIFIC_PATTERN = "specific:%s:perform"; // specific:specificId:operation



    public String buildPermission(PermissionType type, String target, int value) {

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

    public String buildPermission(PermissionType type, String target) {



        return String.format();
    }

    public String buildPermissionByWindowAlias(String windowAlias) {
        return String.format(PERMISSION_WINDOW_PATTERN, windowAlias);
    }

    public String buildPermissionByEntity(MetaClass metaClass, EntityOp entityOp) {
        return String.format(PERMISSION_ENTITY_PATTERN,
                             metaClass.getName(), "*", entityOp.getId());
    }

    public String buildPermissionByEntity(MetaClass metaClass, String operation) {
        return String.format(PERMISSION_ENTITY_PATTERN,
                             metaClass.getName(), "*", operation);
    }

    public String buildPermissionByEntityAttribute(MetaClass metaClass, String property,
                                                   EntityAttrAccess access) {
        return String.format(PERMISSION_ENTITY_ATTRIBUTE_PATTERN,
                             metaClass.getName(), "*", property, "*", access.getId());

    }

    public String buildPermissionByEntityAttribute(MetaClass metaClass, String property,
                                                   String access) {
        return String.format(PERMISSION_ENTITY_ATTRIBUTE_PATTERN,
                             metaClass.getName(), "*", property, "*", access);

    }

    public String buildPermissionBySpecificPermission(String name) {
        return String.format(PERMISSION_SPECIFIC_PATTERN, name);
    }

    public List<String> buildWildcardsPermissionsBasedOn(String permission) {
        return null;
    }
}
