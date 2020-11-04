package ru.udya.sharedsession.redis.permission.repository

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UserSessionSource
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.entity.EntityAttrAccess
import com.haulmont.cuba.security.entity.EntityOp
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.domain.SharedUserSession
import ru.udya.sharedsession.permission.domain.SharedUserPermission
import ru.udya.sharedsession.redis.RedisSharedUserSessionRepository
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserPermissionRepository

import static ru.udya.sharedsession.permission.domain.SharedUserPermission.*
import static ru.udya.sharedsession.redis.RedisSharedUserSessionRepository.KEY_PATTERN

class RedisSharedUserPermissionRepositoryTest extends SharedSessionIntegrationSpecification {

    RedisSharedUserPermissionRepository testClass

    SharedUserSession sharedUserSession

    RedisSharedUserSessionRepository redisSharedUserSessionRepository

    UserSessionSource uss

    void setup() {
        def sharedUserSessionId = String.format(KEY_PATTERN, UuidProvider.createUuid(),
                                                UuidProvider.createUuid())

        sharedUserSession = new SharedUserSessionImpl(sharedUserSessionId)
        uss = AppBeans.get(UserSessionSource)

        redisSharedUserSessionRepository = AppBeans.get(RedisSharedUserSessionRepository)
        testClass = AppBeans.get(RedisSharedUserPermissionRepository)
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
        testClass.addToUserSession(sharedUserSession, permission)

        then:
        testClass.findAllByUserSession(sharedUserSession) == [permission]

        when:
        testClass.addToUserSession(sharedUserSession, [permission2, permission3])

        then:
        testClass.findAllByUserSession(sharedUserSession).toSet() == [permission, permission2, permission3].toSet()
    }

    def "check that user has permission if it is added"() {
        given:
        SharedUserPermission permission =
                entityPermission('sec$User', WILDCARD, EntityOp.CREATE.id)

        SharedUserPermission permission2 =
                entityPermission('sec$Group', WILDCARD, EntityOp.CREATE.id)


        testClass.addToUserSession(sharedUserSession, permission)

        when:
        def does = testClass.doesHavePermission(sharedUserSession, permission)

        then:
        does

        when:
        does = testClass.doesHavePermissions(sharedUserSession, [permission, permission2])

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

        testClass.addToUserSession(sharedUserSession, [entityPermission, entityAttributePermission,
                                                       specificPermission, screenPermission,
                                                       screenElementPermission])

        when:
        def entityPermissionsByUserSession = testClass
                .findAllEntityPermissionsByUserSession(sharedUserSession)

        def entityAttributePermissionsByUserSession = testClass
                .findAllEntityAttributePermissionsByUserSession(sharedUserSession)

        def specificPermissionsByUserSession = testClass
                .findAllSpecificPermissionsByUserSession(sharedUserSession)

        def screenPermissionsByUserSession = testClass
                .findAllScreenPermissionsByUserSession(sharedUserSession)

        def screenElementPermissionsByUserSession = testClass
                .findAllScreenElementPermissionsByUserSession(sharedUserSession)

        then:
        entityPermissionsByUserSession == [entityPermission]
        entityAttributePermissionsByUserSession == [entityAttributePermission]
        specificPermissionsByUserSession == [specificPermission]
        screenPermissionsByUserSession == [screenPermission]
        screenElementPermissionsByUserSession == [screenElementPermission]
    }

    static class SharedUserSessionImpl implements SharedUserSession {

        protected String sharedId

        SharedUserSessionImpl(String sharedId) {
            this.sharedId = sharedId
        }

        @Override
        Serializable getSharedId() {
            return this.sharedId
        }
    }
}
