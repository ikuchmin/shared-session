package ru.udya.sharedsession.permission.helper;

import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.ALL_ENTITY_ATTRIBUTES_PERMISSIONS;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.ALL_ENTITY_PERMISSIONS;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.ALL_SCREEN_ELEMENT_PERMISSIONS;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.ALL_SCREEN_PERMISSIONS;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.ALL_SPECIFIC_PERMISSIONS;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.WILDCARD;

@Component("ss_SharedUserPermissionWildcardHelper")
public class SharedUserPermissionWildcardHelper {

    public List<SharedUserPermission> buildWildcardPermissions(SharedUserPermission permission) {

        //noinspection unchecked
        return (List<SharedUserPermission>) Match(permission).of(
                Case($(instanceOf(SharedUserEntityPermission.class)),
                     this::buildWildcardEntityPermissions),

                Case($(instanceOf(SharedUserEntityAttributePermission.class)),
                     this::buildWildcardEntityAttributePermissions),

                Case($(instanceOf(SharedUserSpecificPermission.class)),
                     this::buildWildcardSpecificPermissions),

                Case($(instanceOf(SharedUserScreenPermission.class)),
                     this::buildWildcardScreenPermissions),

                Case($(instanceOf(SharedUserScreenElementPermission.class)),
                     this::buildWildcardScreenElementPermissions),

                Case($(), p -> Collections.emptyList())
        );
    }

    public List<SharedUserEntityPermission> buildWildcardEntityPermissions(
            SharedUserEntityPermission permission) {

        return Stream.of(
                ALL_ENTITY_PERMISSIONS,

                SharedUserPermission.entityPermission(
                        permission.getEntityType(), WILDCARD, WILDCARD),

                SharedUserPermission.entityPermission(
                        permission.getEntityType(), permission.getEntityId(), WILDCARD),

                SharedUserPermission.entityPermission(
                        permission.getEntityType(), WILDCARD, permission.getOperation()),

                SharedUserPermission.entityPermission(
                        WILDCARD, WILDCARD, permission.getOperation())
        ).distinct().collect(Collectors.toList());
    }

    public List<SharedUserEntityAttributePermission> buildWildcardEntityAttributePermissions(
            SharedUserEntityAttributePermission permission) {

        return Stream.of(
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
        ).distinct().collect(Collectors.toList());
    }

    public List<SharedUserSpecificPermission> buildWildcardSpecificPermissions(
            SharedUserSpecificPermission permission) {

        return Stream.of(
                ALL_SPECIFIC_PERMISSIONS,

                SharedUserPermission.specificPermission(
                        permission.getSpecificPermissionId(), WILDCARD
                ),

                SharedUserPermission.specificPermission(
                        WILDCARD, permission.getOperation()
                )
        ).distinct().collect(Collectors.toList());
    }

    public List<SharedUserScreenPermission> buildWildcardScreenPermissions(
            SharedUserScreenPermission permission) {

        return Stream.of(
                ALL_SCREEN_PERMISSIONS,

                SharedUserPermission.screenPermission(
                        permission.getScreenId(), WILDCARD),

                SharedUserPermission.screenPermission(
                        WILDCARD, permission.getOperation()
                )
        ).distinct().collect(Collectors.toList());
    }

    public List<SharedUserScreenElementPermission> buildWildcardScreenElementPermissions(
            SharedUserScreenElementPermission permission) {

        return Stream.of(
                ALL_SCREEN_ELEMENT_PERMISSIONS,

                SharedUserPermission.screenElementPermission(
                        permission.getScreenId(), WILDCARD, WILDCARD),

                SharedUserPermission.screenElementPermission(
                        permission.getScreenId(), permission.getScreenElementId(), WILDCARD),

                SharedUserPermission.screenElementPermission(
                        WILDCARD, WILDCARD, permission.getOperation()
                )
        ).distinct().collect(Collectors.toList());
    }
}
