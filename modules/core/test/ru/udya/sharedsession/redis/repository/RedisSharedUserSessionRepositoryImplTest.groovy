package ru.udya.sharedsession.redis.repository

import com.haulmont.cuba.core.entity.contracts.Id
import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UserSessionSource
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.entity.User
import com.haulmont.cuba.security.global.UserSession
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId

class RedisSharedUserSessionRepositoryImplTest extends SharedSessionIntegrationSpecification {

    RedisSharedUserSessionRepositoryImpl testClass

    UserSessionSource uss

//    RedisSharedUserSessionId sharedUserSessionId

    void setup() {
//        def sharedUserSessionId = String.format(KEY_PATTERN, UuidProvider.createUuid(),
//                                                UuidProvider.createUuid())
//
//        this.sharedUserSessionId = RedisSharedUserSessionId.of(sharedUserSessionId)

        uss = AppBeans.get(UserSessionSource)

        testClass = AppBeans.get(RedisSharedUserSessionRepositoryImpl)
    }

    def "check that finding user sessions by shared id works as well"() {
        def usId = UuidProvider.createUuid()

        def user = new User()
        user.with {
            id = UuidProvider.createUuid()
        }

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        def redisUserSession = testClass.createByCubaUserSession(us)

        when:
        def loadedRedisUserSession = testClass.findById(redisUserSession)

        then:
        redisUserSession == loadedRedisUserSession
    }

    def "check that finding user sessions by userId works as well"() {
        def usId = UuidProvider.createUuid()
        def us2Id = UuidProvider.createUuid()

        def user = new User()
        user.with {
            id = UuidProvider.createUuid()
        }

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)
        def us2 = new UserSession(us2Id, user, [], uss.getUserSession().getLocale(), false)

        def redisUserSession = testClass.createByCubaUserSession(us)
        def redisUserSession2 = testClass.createByCubaUserSession(us2)

        def createdSharedIds = [redisUserSession, redisUserSession2]
                .collect { RedisSharedUserSessionId.of(it.sharedId) }
        when:
        def sharedIds = testClass.findAllIdsByUser(Id.of(user))

        then:
        createdSharedIds.toSet() == sharedIds.toSet()
    }

    def "check that finding user session by cuba UserSession id works as well"() {
        def usId = UuidProvider.createUuid()

        def user = new User()
        user.with {
            id = UuidProvider.createUuid()
        }

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        def redisUserSession = testClass.createByCubaUserSession(us)

        when:
        def sharedUserSessionId = testClass.findIdByCubaUserSessionId(us.id)

        then:
        RedisSharedUserSessionId.of(redisUserSession.sharedId) == sharedUserSessionId
    }
}
