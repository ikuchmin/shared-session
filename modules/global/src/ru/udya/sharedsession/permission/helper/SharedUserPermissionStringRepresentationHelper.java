package ru.udya.sharedsession.permission.helper;

import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.domain.SharedUserEntityAttributePermission;
import ru.udya.sharedsession.permission.domain.SharedUserEntityPermission;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenElementPermission;
import ru.udya.sharedsession.permission.domain.SharedUserScreenPermission;
import ru.udya.sharedsession.permission.domain.SharedUserSpecificPermission;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;

@Component("ss_SharedUserPermissionStringRepresentationHelper")
public class SharedUserPermissionStringRepresentationHelper {

    public static final String DELIMITER = ":";

    public static final String PERMISSION_ENTITY_PREFIX = "entity";

    public static final String PERMISSION_ENTITY_ATTRIBUTE_PREFIX = "entity_attribute";

    public static final String PERMISSION_SPECIFIC_PREFIX = "specific";

    public static final String PERMISSION_SCREEN_PREFIX = "screen";

    public static final String PERMISSION_SCREEN_ELEMENT_PREFIX = "screen_element";


    // entity:entityType:entityId:operation
    public static final String PERMISSION_ENTITY_PATTERN =
            PERMISSION_ENTITY_PREFIX + ":" + "%s:%s:%s";

    // entity_attribute:entityType:entityId:entityAttribute:entityAttributeValue:operation
    public static final String PERMISSION_ENTITY_ATTRIBUTE_PATTERN =
            PERMISSION_ENTITY_ATTRIBUTE_PREFIX + ":" + "%s:%s:%s:%s:%s";

    // specific:specificId:operation
    public static final String PERMISSION_SPECIFIC_PATTERN =
            PERMISSION_SPECIFIC_PREFIX + ":" + "%s:%s";

    // screen:screenId:operation
    public static final String PERMISSION_SCREEN_PATTERN =
            PERMISSION_SCREEN_PREFIX + ":" + "%s:%s";

    // screen:screenId:operation
    public static final String PERMISSION_SCREEN_ELEMENT_PATTERN =
            PERMISSION_SCREEN_ELEMENT_PREFIX + ":" + "%s:%s:%s";

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

                Case($(instanceOf(SharedUserScreenElementPermission.class)),
                     this::convertScreenElementPermissionToString),

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

    public String convertScreenElementPermissionToString(
            SharedUserScreenElementPermission screenElementPermission) {

        return String.format(PERMISSION_SCREEN_ELEMENT_PATTERN,
                             screenElementPermission.getScreenId(),
                             screenElementPermission.getScreenElementId(),
                             screenElementPermission.getOperation());
    }

    public SharedUserPermission convertStringToPermission(String stringRepresentation) {

        var stringRepresentationParts = stringRepresentation.split(DELIMITER);

        return Match(stringRepresentationParts[0]).of(
                Case($(PERMISSION_ENTITY_PREFIX), t -> convertStringToEntityPermission(stringRepresentation)),
                Case($(PERMISSION_ENTITY_ATTRIBUTE_PREFIX), t -> convertStringToEntityAttributePermission(stringRepresentation)),
                Case($(PERMISSION_SPECIFIC_PREFIX), t -> convertStringToSpecificPermission(stringRepresentation)),
                Case($(PERMISSION_SCREEN_PREFIX), t -> convertStringToScreenPermission(stringRepresentation)),
                Case($(PERMISSION_SCREEN_ELEMENT_PREFIX), t -> convertStringToScreenElementPermission(stringRepresentation)),
                Case($(), t -> { throw new IllegalArgumentException(String.format("Permission type isn't supported (type: %s)", t)); })
        );

    }

    public SharedUserEntityPermission convertStringToEntityPermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(DELIMITER);

        return SharedUserPermission.entityPermission(stringRepresentationParts[1],
                                                     stringRepresentationParts[2],
                                                     stringRepresentationParts[3]);
    }

    public SharedUserEntityAttributePermission convertStringToEntityAttributePermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(DELIMITER);

        return SharedUserPermission.entityAttributePermission(stringRepresentationParts[1],
                                                              stringRepresentationParts[2],
                                                              stringRepresentationParts[3],
                                                              stringRepresentationParts[4],
                                                              stringRepresentationParts[5]);
    }

    public SharedUserSpecificPermission convertStringToSpecificPermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(DELIMITER);

        return SharedUserPermission.specificPermission(stringRepresentationParts[1],
                                                       stringRepresentationParts[2]);
    }

    public SharedUserScreenPermission convertStringToScreenPermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(DELIMITER);

        return SharedUserPermission.screenPermission(stringRepresentationParts[1],
                                                     stringRepresentationParts[2]);
    }

    public SharedUserScreenElementPermission convertStringToScreenElementPermission(String stringRepresentation) {
        var stringRepresentationParts = stringRepresentation.split(DELIMITER);

        return SharedUserPermission.screenElementPermission(stringRepresentationParts[1],
                                                            stringRepresentationParts[2],
                                                            stringRepresentationParts[3]);
    }

    public String defineTypeStringPrefixByPermissionType(Class<? extends SharedUserPermission> type) {
        return Match(type).of(
                Case($(t -> t == SharedUserEntityPermission.class), t -> PERMISSION_ENTITY_PREFIX),
                Case($(t -> t == SharedUserEntityAttributePermission.class), t -> PERMISSION_ENTITY_ATTRIBUTE_PREFIX),
                Case($(t -> t == SharedUserSpecificPermission.class), t -> PERMISSION_SPECIFIC_PREFIX),
                Case($(t -> t == SharedUserScreenPermission.class), t -> PERMISSION_SCREEN_PREFIX),
                Case($(t -> t == SharedUserScreenElementPermission.class), t -> PERMISSION_SCREEN_ELEMENT_PREFIX),
                Case($(), t -> { throw new IllegalArgumentException("Permission type isn't supported"); }));
    }
}
