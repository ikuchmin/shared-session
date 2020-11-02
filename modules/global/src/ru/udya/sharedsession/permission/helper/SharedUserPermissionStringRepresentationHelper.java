package ru.udya.sharedsession.permission.helper;

import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;

@Component("ss_SharedUserPermissionStringRepresentationHelper")
public class SharedUserPermissionStringRepresentationHelper {

    // entity:entityType:entityId:operation
    public static final String PERMISSION_ENTITY_PATTERN = "entity:%s:%s:%s";

    // entity_attribute:entityType:entityId:entityAttribute:entityAttributeValue:operation
    public static final String PERMISSION_ENTITY_ATTRIBUTE_PATTERN = "entity_attribute:%s:%s:%s:%s:%s";

    // specific:specificId:operation
    public static final String PERMISSION_SPECIFIC_PATTERN = "specific:%s:%s";

    // screen:screenId:operation
    public static final String PERMISSION_SCREEN_PATTERN = "screen:%s:%s";

    public String convertPermissionToString(SharedUserPermission permission) {
        return Match(permission).of(
                Case($(instanceOf(SharedUserEntityPermission.class)),
                     this::convertEntityPermissionToString),

                Case($(instanceOf(SharedUserEntityAttributePermission.class)),
                     this::convertEntityAttributePermissionToString),

                Case($(instanceOf(SharedUserSpecificPermission.class)),
                     this::convertSpecificPermissionToString),

                Case($(instanceOf(SharedUserScreenPermission.class)),
                     this::convertScreenPermissionToString),

                Case($(), p -> { throw new IllegalArgumentException(String.format("Permission type isn't supported (%s)", p)); })
        );
    }

    public String convertEntityPermissionToString(SharedUserEntityPermission entityPermission) {
        return String.format(PERMISSION_ENTITY_PATTERN, entityPermission.getEntityType(),
                             entityPermission.getEntityId(), entityPermission.getOperation());
    }

    public String convertEntityAttributePermissionToString(
            SharedUserEntityAttributePermission entityAttributePermission) {

        return String.format(PERMISSION_ENTITY_ATTRIBUTE_PATTERN,
                             entityAttributePermission.getEntityType(),
                             entityAttributePermission.getEntityId(),
                             entityAttributePermission.getEntityAttribute(),
                             entityAttributePermission.getEntityAttributeValue(),
                             entityAttributePermission.getOperation());
    }

    public String convertSpecificPermissionToString(
            SharedUserSpecificPermission specificPermission) {

        return String.format(PERMISSION_SPECIFIC_PATTERN,
                             specificPermission.getSpecificPermissionId(),
                             specificPermission.getOperation());
    }

    public String convertScreenPermissionToString(
            SharedUserScreenPermission screenPermission) {

        return String.format(PERMISSION_SCREEN_PATTERN,
                             screenPermission.getScreenId(),
                             screenPermission.getOperation());
    }

    public SharedUserPermission convertStringToPermission(String stringRepresentation) {

        var stringRepresentationParts = stringRepresentation.split(":");

        return Match(stringRepresentationParts[0]).of(
                Case($("entity"), t -> convertStringToEntityPermission(stringRepresentation)),
                Case($("entity_attribute"), t -> convertStringToEntityAttributePermission(stringRepresentation)),
                Case($("specific"), t -> convertStringToSpecificPermission(stringRepresentation)),
                Case($("screen"), t -> convertStringToScreenPermission(stringRepresentation)),
                Case($(), t -> { throw new IllegalArgumentException(String.format("Permission type isn't supported (type: %s)", t)); })
        );

    }

    public SharedUserEntityPermission convertStringToEntityPermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(":");

        return SharedUserPermission.entityPermission(stringRepresentationParts[1],
                                                     stringRepresentationParts[2],
                                                     stringRepresentationParts[3]);
    }

    public SharedUserEntityAttributePermission convertStringToEntityAttributePermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(":");

        return SharedUserPermission.entityAttributePermission(stringRepresentationParts[1],
                                                              stringRepresentationParts[2],
                                                              stringRepresentationParts[3],
                                                              stringRepresentationParts[4],
                                                              stringRepresentationParts[5]);
    }

    public SharedUserSpecificPermission convertStringToSpecificPermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(":");

        return SharedUserPermission.specificPermission(stringRepresentationParts[1],
                                                       stringRepresentationParts[2]);
    }

    public SharedUserScreenPermission convertStringToScreenPermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(":");

        return SharedUserPermission.screenPermission(stringRepresentationParts[1],
                                                     stringRepresentationParts[2]);
    }
}
