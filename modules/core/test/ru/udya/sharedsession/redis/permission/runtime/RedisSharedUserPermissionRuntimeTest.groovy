package ru.udya.sharedsession.redis.permission.runtime

import com.haulmont.cuba.core.entity.contracts.Id
import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.entity.EntityOp
import com.haulmont.cuba.security.entity.User
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.permission.domain.SharedUserPermission
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserPermissionRepository

import static ru.udya.sharedsession.permission.domain.SharedUserPermission.*
import static ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository.KEY_PATTERN

class RedisSharedUserPermissionRuntimeTest extends SharedSessionIntegrationSpecification {

    RedisSharedUserPermissionRuntime testClass

    RedisSharedUserPermissionRepository permissionRepository

    RedisSharedUserSessionId sharedUserSession

    void setup() {
        def sharedUserSessionId = String.format(KEY_PATTERN, UuidProvider.createUuid(),
                                                UuidProvider.createUuid())

        sharedUserSession = RedisSharedUserSessionId.of(sharedUserSessionId)

        permissionRepository = AppBeans.get(RedisSharedUserPermissionRepository)

        testClass = AppBeans.get(RedisSharedUserPermissionRuntime)
    }

    def "check that all entity permissions granted can be proved by concrete permissions"() {
        given:
        SharedUserPermission permission =
                entityPermission(entityType, entityId, operation)

        permissionRepository.addToUserSession(sharedUserSession, ALL_ENTITY_PERMISSIONS)

        when:
        def isGranted = testClass.isPermissionGrantedToUserSession(sharedUserSession, permission)

        then:
        isGranted == result

        where:
        entityType | entityId                            | operation          | result
        WILDCARD   | WILDCARD                            | WILDCARD           | true
        WILDCARD   | WILDCARD                            | EntityOp.READ.id   | true
        'sec$User' | WILDCARD                            | EntityOp.CREATE.id | true
        'sec$User' | UuidProvider.createUuid() as String | EntityOp.CREATE.id | true
    }

    def "check that entity permissions granted on User can be proved only by User permissions"() {
        given:
        SharedUserPermission permission =
                entityPermission(entityType, entityId, operation)

        SharedUserPermission grantedPermission =
                entityPermission('sec$User', WILDCARD, WILDCARD)
        permissionRepository.addToUserSession(sharedUserSession, grantedPermission)

        when:
        def isGranted = testClass.isPermissionGrantedToUserSession(sharedUserSession, permission)

        then:
        isGranted == result

        where:
        entityType | entityId                            | operation          | result
        WILDCARD   | WILDCARD                            | WILDCARD           | false
        WILDCARD   | WILDCARD                            | EntityOp.READ.id   | false
        'sec$Group'| WILDCARD                            | EntityOp.READ.id   | false
        'sec$User' | WILDCARD                            | EntityOp.READ.id   | true
        'sec$User' | UuidProvider.createUuid() as String | EntityOp.CREATE.id | true
    }

    def "check that entity permissions granted on concrete User can be proved only by concrete User permissions"() {
        given:
        SharedUserPermission permission =
                entityPermission(entityType, entityId, operation)

        SharedUserPermission grantedPermission =
                entityPermission('sec$User', "9270d6a2-c38d-c3da-1dac-70d54143b762", "assignToGroup")

        permissionRepository.addToUserSession(sharedUserSession, grantedPermission)

        when:
        def isGranted = testClass.isPermissionGrantedToUserSession(sharedUserSession, permission)

        then:
        isGranted == result

        where:
        entityType  | entityId                               | operation          | result
        WILDCARD    | WILDCARD                               | WILDCARD           | false
        WILDCARD    | WILDCARD                               | EntityOp.READ.id   | false
        'sec$Group' | WILDCARD                               | EntityOp.READ.id   | false
        'sec$User'  | WILDCARD                               | EntityOp.READ.id   | false
        'sec$User'  | "9270d6a2-c38d-c3da-1dac-70d54143b762" | EntityOp.CREATE.id | false
        'sec$User'  | "9270d6a2-c38d-c3da-1dac-70d54143b762" | "assignToGroup"    | true
    }

    def "check that user has granted permission after someone grants them to him"() {
        given:
        SharedUserPermission grantedPermission =
                entityPermission('sec$User', "9270d6a2-c38d-c3da-1dac-70d54143b762", "assignToGroup")

        when:
        testClass.grantPermissionToUserSession(sharedUserSession, grantedPermission)

        then:
        testClass.isPermissionGrantedToUserSession(sharedUserSession, grantedPermission)
    }

    def "check that user has granted permission in all sessions after someone grants them to him"() {
        given:

        def userId = UuidProvider.createUuid()
        def firstSharedUserSessionId = String.format(KEY_PATTERN, userId, UuidProvider.createUuid())
        def firstSharedUserSession = RedisSharedUserSessionId.of(firstSharedUserSessionId)

        def secondSharedUserSessionId = String.format(KEY_PATTERN, userId, UuidProvider.createUuid())
        def secondSharedUserSession = RedisSharedUserSessionId.of(secondSharedUserSessionId)

        SharedUserPermission grantedPermission =
                entityPermission('sec$User', "9270d6a2-c38d-c3da-1dac-70d54143b762", "assignToGroup")

        testClass.grantPermissionToUserSessions([firstSharedUserSession, secondSharedUserSession],
                                                grantedPermission)

        SharedUserPermission anotherPermission =
                entityPermission('sec$User', "9270d6a2-c38d-c3da-1dac-70d54143b762", "assignToGroup")
        when:
        testClass.grantPermissionToAllUserSessions(Id.of(userId, User), grantedPermission)

        then:
        testClass.isPermissionGrantedToUserSession(firstSharedUserSession, grantedPermission)
        testClass.isPermissionGrantedToUserSession(secondSharedUserSession, grantedPermission)
    }
}
