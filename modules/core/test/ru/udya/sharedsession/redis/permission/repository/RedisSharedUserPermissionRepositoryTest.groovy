package ru.udya.sharedsession.redis.permission.repository

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.entity.EntityAttrAccess
import com.haulmont.cuba.security.entity.EntityOp
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.permission.domain.SharedUserPermission
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId

import static ru.udya.sharedsession.permission.domain.SharedUserPermission.*
import static ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository.KEY_PATTERN

class RedisSharedUserPermissionRepositoryTest extends SharedSessionIntegrationSpecification {

    RedisSharedUserSessionPermissionRepository testClass

    RedisSharedUserSessionId sharedUserSessionId

    void setup() {
        def sharedUserSessionId = String.format(KEY_PATTERN, UuidProvider.createUuid(),
                                                UuidProvider.createUuid())

        this.sharedUserSessionId = RedisSharedUserSessionId.of(sharedUserSessionId)

        testClass = AppBeans.get(RedisSharedUserSessionPermissionRepository)
    }

    def "check that add permissions to user works as well"() {
        given:
        SharedUserPermission permission =
                entityPermission('sec$User', WILDCARD, EntityOp.CREATE.id)

        SharedUserPermission permission2 =
                entityPermission('sec$Group', WILDCARD, EntityOp.CREATE.id)

        SharedUserPermission permission3 =
                entityPermission('sec$User', WILDCARD, EntityOp.READ.id)
        when:
        testClass.addToUserSession(sharedUserSessionId, permission)

        then:
        testClass.findAllByUserSession(sharedUserSessionId) == [permission]

        when:
        testClass.addToUserSession(sharedUserSessionId, [permission2, permission3])

        then:
        testClass.findAllByUserSession(sharedUserSessionId).toSet() == [permission, permission2, permission3].toSet()
    }

    def "check that user has permission if it is added"() {
        given:
        SharedUserPermission permission =
                entityPermission('sec$User', WILDCARD, EntityOp.CREATE.id)

        SharedUserPermission permission2 =
                entityPermission('sec$Group', WILDCARD, EntityOp.CREATE.id)


        testClass.addToUserSession(sharedUserSessionId, permission)

        when:
        def does = testClass.doesHavePermission(sharedUserSessionId, permission)

        then:
        does

        when:
        does = testClass.doesHavePermissions(sharedUserSessionId, [permission, permission2])

        then:
        does == [true, false]
    }

    def "check that finding permission by type works as well"() {
        given:
        SharedUserPermission entityPermission =
                entityPermission('sec$User', WILDCARD, EntityOp.CREATE.id)

        SharedUserPermission entityAttributePermission =
                entityAttributePermission('sec$User', WILDCARD, 'lastName', WILDCARD,
                                          EntityAttrAccess.MODIFY.name().toLowerCase())

        SharedUserPermission specificPermission =
                specificPermission('specific_permission', WILDCARD)

        SharedUserPermission screenPermission =
                screenPermission('sec$User.edit', WILDCARD)

        SharedUserPermission screenElementPermission =
                screenElementPermission('sec$User.edit', 'commitBtn', WILDCARD)

        testClass.addToUserSession(sharedUserSessionId, [entityPermission, entityAttributePermission,
                                                         specificPermission, screenPermission,
                                                         screenElementPermission])

        when:
        def entityPermissionsByUserSession = testClass
                .findAllEntityPermissionsByUserSession(sharedUserSessionId)

        def entityAttributePermissionsByUserSession = testClass
                .findAllEntityAttributePermissionsByUserSession(sharedUserSessionId)

        def specificPermissionsByUserSession = testClass
                .findAllSpecificPermissionsByUserSession(sharedUserSessionId)

        def screenPermissionsByUserSession = testClass
                .findAllScreenPermissionsByUserSession(sharedUserSessionId)

        def screenElementPermissionsByUserSession = testClass
                .findAllScreenElementPermissionsByUserSession(sharedUserSessionId)

        then:
        entityPermissionsByUserSession == [entityPermission]
        entityAttributePermissionsByUserSession == [entityAttributePermission]
        specificPermissionsByUserSession == [specificPermission]
        screenPermissionsByUserSession == [screenPermission]
        screenElementPermissionsByUserSession == [screenElementPermission]
    }
}
