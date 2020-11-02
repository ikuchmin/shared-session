package ru.udya.sharedsession.permission.helper;

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

@Component("ss_SharedUserPermissionWildcardHelper")
public class SharedUserPermissionWildcardHelper {

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
