package ru.udya.sharedsession.permission.domain;

public interface SharedUserPermission {

    String WILDCARD = "*";

    SharedUserEntityPermission ALL_ENTITY_PERMISSIONS =
            entityPermission(WILDCARD, WILDCARD, WILDCARD);

    SharedUserEntityAttributePermission ALL_ENTITY_ATTRIBUTES_PERMISSIONS =
            entityAttributePermission(WILDCARD, WILDCARD, WILDCARD, WILDCARD, WILDCARD);

    SharedUserSpecificPermission ALL_SPECIFIC_PERMISSIONS =
            specificPermission(WILDCARD, WILDCARD);

    SharedUserScreenPermission ALL_SCREEN_PERMISSIONS =
            screenPermission(WILDCARD, WILDCARD);

    static SharedUserEntityPermission entityPermission(String entityType,
                                                       String entityId,
                                                       String operation) {

        var permission = new SharedUserEntityPermission();

        permission.entityType = entityType;
        permission.entityId = entityId;
        permission.operation = operation;

        if (WILDCARD.equals(permission.entityType)
            && (! WILDCARD.equals(permission.entityId))) {

            throw new IllegalArgumentException("EntityType mustn't be wildcard when entityId isn't wildcard (*:entityId:* isn't allowed)");
        }

        return permission;
    }

    static SharedUserEntityAttributePermission entityAttributePermission(String entityType, String entityId,
                                                                         String entityAttribute,
                                                                         String entityAttributeValue,
                                                                         String operation) {

        var permission = new SharedUserEntityAttributePermission();

        permission.entityType = entityType;
        permission.entityId = entityId;
        permission.entityAttribute = entityAttribute;
        permission.entityAttributeValue = entityAttributeValue;
        permission.operation = operation;

        return permission;
    }

    static SharedUserSpecificPermission specificPermission(String specificPermissionId,
                                                           String operation) {

        var permission = new SharedUserSpecificPermission();

        permission.specificPermissionId = specificPermissionId;
        permission.operation = operation;

        return permission;
    }

    static SharedUserScreenPermission screenPermission(String screenId,
                                                       String operation) {

        var permission = new SharedUserScreenPermission();

        permission.screenId = screenId;
        permission.operation = operation;

        return permission;
    }
}
