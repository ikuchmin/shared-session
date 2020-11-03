package ru.udya.sharedsession.permission.helper;

import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.security.entity.ScreenComponentPermission;
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
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.WILDCARD;

@Component("ss_CubaPermissionStringRepresentationHelper")
public class CubaPermissionStringRepresentationHelper {

    public static final String CUBA_PERMISSION_ENTITY_PATTERN = "%s:%s";
    public static final String CUBA_PERMISSION_ENTITY_ATTRIBUTE_PATTERN = "%s:%s";
    public static final String CUBA_PERMISSION_SPECIFIC_PATTERN = "%s";
    public static final String CUBA_PERMISSION_SCREEN_PATTERN = "%s";
    public static final String CUBA_PERMISSION_UI_PATTERN = "%s:%s";


    public SharedUserPermission convertCubaPermissionToSharedUserPermission(PermissionType type, String cubaTarget, int value) {
        SharedUserPermission builtPermission;
        switch (type) {
            case SCREEN:
                builtPermission = convertCubaScreenPermissionToSharedUserPermission(cubaTarget, value);
                break;
            case ENTITY_OP:
                builtPermission = convertCubaEntityPermissionToSharedUserPermission(cubaTarget, value);
                break;
            case ENTITY_ATTR:
                builtPermission = convertCubaEntityAttributePermissionToSharedUserPermission(cubaTarget, value);
                break;
            case SPECIFIC:
                builtPermission = convertCubaSpecificSpecificToSharedUserPermission(cubaTarget, value);
                break;
            case UI:
                builtPermission = convertCubaUIPermissionToSharedUserPermission(cubaTarget, value);
                break;
            default:
                throw new IllegalArgumentException(String.format("Permission type (%s) isn't supported", type));
        }

        return builtPermission;
    }

    public SharedUserEntityPermission convertCubaEntityPermissionToSharedUserPermission(String cubaTarget, int value) {
        var targetParts = cubaTarget.split(cubaTarget);
        return SharedUserPermission.entityPermission(targetParts[0], WILDCARD, targetParts[1]);
    }

    public SharedUserEntityAttributePermission convertCubaEntityAttributePermissionToSharedUserPermission(String cubaTarget, int value) {
        var targetParts = cubaTarget.split(cubaTarget);
        return SharedUserPermission.entityAttributePermission(targetParts[0], WILDCARD,
                                                              targetParts[1], WILDCARD,
                                                              EntityAttrAccess.fromId(value)
                                                                              .name().toLowerCase());
    }

    public SharedUserSpecificPermission convertCubaSpecificSpecificToSharedUserPermission(String cubaTarget, int value) {
        return SharedUserPermission.specificPermission(cubaTarget, WILDCARD);
    }

    public SharedUserScreenPermission convertCubaScreenPermissionToSharedUserPermission(String cubaTarget, int value) {
        return SharedUserPermission.screenPermission(cubaTarget, WILDCARD);
    }

    public SharedUserScreenElementPermission convertCubaUIPermissionToSharedUserPermission(String cubaTarget, int value) {
        var targetParts = cubaTarget.split(cubaTarget);
        return SharedUserPermission.screenElementPermission(targetParts[0], targetParts[1],
                                                            ScreenComponentPermission.fromId(value)
                                                                                     .name().toLowerCase());
    }

    public CubaPermission convertSharedUserPermissionToCubaPermission(SharedUserPermission sharedUserPermission) {
        return Match(sharedUserPermission).of(
                Case($(instanceOf(SharedUserEntityPermission.class)),
                     this::convertSharedUserEntityPermissionToCubaPermission),

                Case($(instanceOf(SharedUserEntityAttributePermission.class)),
                     this::convertSharedUserEntityAttributePermissionToCubaPermission),

                Case($(instanceOf(SharedUserSpecificPermission.class)),
                     this::convertSharedUserSpecificPermissionToCubaPermission),

                Case($(instanceOf(SharedUserScreenPermission.class)),
                     this::convertSharedUserScreenPermissionToCubaPermission),

                Case($(instanceOf(SharedUserScreenElementPermission.class)),
                     this::convertSharedUserScreenElementPermissionToCubaPermission),

                Case($(), p -> { throw new IllegalArgumentException(String.format("Permission type isn't supported (type: %s)", p)); })
        );

    }

    public CubaPermission convertSharedUserEntityPermissionToCubaPermission(SharedUserEntityPermission entityPermission) {
        var cubaPermissionTarget = String.format(CUBA_PERMISSION_ENTITY_PATTERN,
                                                 entityPermission.getEntityType(),
                                                 entityPermission.getOperation());

        return new CubaPermission(PermissionType.ENTITY_OP, cubaPermissionTarget, 1);
    }

    public CubaPermission convertSharedUserEntityAttributePermissionToCubaPermission(SharedUserEntityAttributePermission entityAttributePermission) {
        var cubaPermissionTarget = String.format(CUBA_PERMISSION_ENTITY_ATTRIBUTE_PATTERN,
                                                 entityAttributePermission.getEntityType(),
                                                 entityAttributePermission.getEntityAttribute());

        var entityAttrAccess = EntityAttrAccess
                .valueOf(entityAttributePermission.getOperation().toUpperCase());

        return new CubaPermission(PermissionType.ENTITY_ATTR, cubaPermissionTarget, entityAttrAccess.getId());
    }

    public CubaPermission convertSharedUserSpecificPermissionToCubaPermission(SharedUserSpecificPermission specificPermission) {
        var cubaPermissionTarget = String.format(CUBA_PERMISSION_SPECIFIC_PATTERN,
                                                 specificPermission.getSpecificPermissionId());

        return new CubaPermission(PermissionType.SPECIFIC, cubaPermissionTarget, 1);
    }

    public CubaPermission convertSharedUserScreenPermissionToCubaPermission(SharedUserScreenPermission screenPermission) {
        var cubaPermissionTarget = String.format(CUBA_PERMISSION_SCREEN_PATTERN,
                                                 screenPermission.getScreenId());

        return new CubaPermission(PermissionType.SCREEN, cubaPermissionTarget, 1);
    }

    public CubaPermission convertSharedUserScreenElementPermissionToCubaPermission(SharedUserScreenElementPermission screenElementPermission) {
        var cubaPermissionTarget = String.format(CUBA_PERMISSION_UI_PATTERN,
                                                 screenElementPermission.getScreenId(),
                                                 screenElementPermission.getScreenElementId());

        var screenComponentPermission = ScreenComponentPermission
                .valueOf(screenElementPermission.getOperation().toUpperCase());

        return new CubaPermission(PermissionType.UI, cubaPermissionTarget, screenComponentPermission.getId());
    }


    public static class CubaPermission {

        protected PermissionType permissionType;

        protected String target;

        protected Integer value;

        public CubaPermission() {
        }

        public CubaPermission(PermissionType permissionType, String target, Integer value) {
            this.permissionType = permissionType;
            this.target = target;
            this.value = value;
        }

        public PermissionType getPermissionType() {
            return permissionType;
        }

        public void setPermissionType(PermissionType permissionType) {
            this.permissionType = permissionType;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }
}
