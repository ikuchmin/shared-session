package ru.udya.sharedsession.permission.helper;

import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.ScreenComponentPermission;
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

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.entityAttributePermission;
import static ru.udya.sharedsession.permission.domain.SharedUserPermission.screenElementPermission;

public class SharedUserPermissionParentHelper {

    protected SharedUserPermissionWildcardHelper wildcardHelper;

    public SharedUserPermissionParentHelper(
            SharedUserPermissionWildcardHelper wildcardHelper) {
        this.wildcardHelper = wildcardHelper;
    }

    public List<SharedUserPermission> calculateParentPermissions(SharedUserPermission permission) {

        //noinspection unchecked
        return (List<SharedUserPermission>) Match(permission).of(
                Case($(instanceOf(SharedUserEntityPermission.class)),
                     wildcardHelper::buildWildcardEntityPermissions),

                Case($(instanceOf(SharedUserEntityAttributePermission.class)),
                     this::calculateParentEntityAttributePermissions),

                Case($(instanceOf(SharedUserSpecificPermission.class)),
                     wildcardHelper::buildWildcardSpecificPermissions),

                Case($(instanceOf(SharedUserScreenPermission.class)),
                     wildcardHelper::buildWildcardScreenPermissions),

                Case($(instanceOf(SharedUserScreenElementPermission.class)),
                     this::calculateParentScreenElementPermissions)
        );
    }

    public List<SharedUserEntityAttributePermission> calculateParentEntityAttributePermissions(
            SharedUserEntityAttributePermission entityAttributePermission) {

        var wildcardEntityAttributePermissions = wildcardHelper
                .buildWildcardEntityAttributePermissions(entityAttributePermission);

        List<SharedUserEntityAttributePermission> wildcardEntityAttributeModifyPermissions = Collections.emptyList();
        if (EntityAttrAccess.VIEW.name().equalsIgnoreCase(entityAttributePermission.getOperation())) {
            wildcardEntityAttributeModifyPermissions = wildcardHelper.buildWildcardEntityAttributePermissions(
                            entityAttributePermission(entityAttributePermission.getEntityType(),
                                                      entityAttributePermission.getEntityAttribute(),
                                                      entityAttributePermission.getEntityAttribute(),
                                                      entityAttributePermission.getEntityAttributeValue(),
                                                      EntityAttrAccess.MODIFY.name().toLowerCase()));

        }

        return Stream.concat(wildcardEntityAttributePermissions.stream(),
                             wildcardEntityAttributeModifyPermissions.stream())
                     .distinct()
                     .collect(Collectors.toList());
    }

    public List<SharedUserScreenElementPermission> calculateParentScreenElementPermissions(
            SharedUserScreenElementPermission screenElementPermission) {

        var wildcardScreenElementPermissions = wildcardHelper
                .buildWildcardScreenElementPermissions(screenElementPermission);

        List<SharedUserScreenElementPermission> wildcardScreenElementModifyPermissions = Collections.emptyList();
        if (ScreenComponentPermission.VIEW.name().equalsIgnoreCase(screenElementPermission.getOperation())) {

            wildcardScreenElementModifyPermissions = wildcardHelper.buildWildcardScreenElementPermissions(
                    screenElementPermission(screenElementPermission.getScreenId(),
                                            screenElementPermission.getScreenElementId(),
                                            ScreenComponentPermission.MODIFY.name().toLowerCase()));
        }

        return Stream.concat(wildcardScreenElementPermissions.stream(),
                             wildcardScreenElementModifyPermissions.stream())
                     .distinct().collect(Collectors.toList());
    }
}
