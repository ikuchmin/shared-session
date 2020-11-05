package ru.udya.sharedsession.redis

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UserSessionSource
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.global.UserSession
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.cache.SharedUserSessionCache

class RedisSharedUserSessionRepositoryTest extends SharedSessionIntegrationSpecification {
    
    RedisSharedUserSessionRuntimeAdapter testClass

    UserSessionSource uss

    SharedUserSessionCache sharedUserSessionCache

    void setup() {
        uss = AppBeans.get(UserSessionSource)
        sharedUserSessionCache = AppBeans.get(SharedUserSessionCache)

        testClass = AppBeans.get(RedisSharedUserSessionRuntimeAdapter)
//        testClass = Spy(RedisSharedUserSessionRuntimeAdapter,
//                        constructorArgs: [AppBeans.get(RedisClient), sharedUserSessionCache,
//                                          AppBeans.get(SharedUserPermissionBuildHelper),
//                                          AppBeans.get(CubaPermissionStringRepresentationHelper),
//                                          AppBeans.get(CubaPermissionBuildHelper),
//                                          AppBeans.get(RedisSharedUserPermissionRuntime),
//                                          AppBeans.get(RedisSharedUserPermissionRepository)])
    }

    def "check that creating shared user session works as well"() {
        given:
        def usId = UuidProvider.createUuid()
        def user = uss.getUserSession().getUser()

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        when:
        def redisUserSession = testClass.createSession(us)

        then:
        redisUserSession.sharedUserSessionId.getSharedId() == "shared:session:$user.id:$usId"
    }

    def "check that finding shared user session works as well"() {
        given:
        def usId = UuidProvider.createUuid()
        def user = uss.getUserSession().getUser()

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        def createdSharedSession = testClass.createSession(us)

        when:
        def redisSharedSession = testClass.findById(createdSharedSession.sharedId)

        then:
        redisSharedSession.getLocale() == us.locale
    }

    def "check that reading values from shared user session doesn't produce query to Redis every time"() {
        given:
        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        def sharedUserSession = testClass.createSession(us)

        and: 'remove object from cache'
        sharedUserSessionCache.removeFromCache(sharedUserSession.sharedId)

        when:
        def redisSharedSession = testClass.findById(sharedUserSession.sharedId)

        and: 'put session to the cache'
        redisSharedSession.getLocale()

        then:
        redisSharedSession.getLocale() == us.locale
        redisSharedSession.getUser() == us.user

        1 * testClass.findBySessionKeyNoCache(_)
    }

    def "check that updating value in user session propagate this to Redis"() {
        given:
        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        def shardeUserSession = testClass.createSession(us)

        when:
        shardeUserSession.locale = null

        then:
        testClass.findBySessionKeyNoCache(shardeUserSession.sharedId).locale == null
    }

//    def "check that updating value flushing cache by invalidate message from Redis"() {
//        given:
//        def redisClient = AppBeans.get(RedisClient)
//        def overridedSharedUserSessionCache = new Resp3BasedSharedUserSessionCache(redisClient) {
//            @Override
//            void saveInCache(String sessionKey, UserSession userSession) {
//                // disable direct saving in cache
//            }
//        }
//        overridedSharedUserSessionCache.init()
//
//        def overridedTestClass = new RedisSharedUserSessionRuntimeAdapter(
//                redisClient, overridedSharedUserSessionCache,
//                AppBeans.get(SharedUserPermissionBuildHelper),
//                AppBeans.get(CubaPermissionStringRepresentationHelper),
//                AppBeans.get(CubaPermissionBuildHelper),
//                AppBeans.get(RedisSharedUserPermissionRuntime),
//                AppBeans.get(RedisSharedUserPermissionRepository))
//        overridedTestClass.init()
//
//        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
//                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks
//
//        when:
//        def sharedUserSession = overridedTestClass.createSession(us)
//
//        then:
//        sharedUserSession.locale == us.locale
//
//        when:
//        sharedUserSession.locale = null
//
//        and: 'waiting'
//        Thread.sleep(200)
//
//        then:
//        sharedUserSession.locale == null
//
//        cleanup:
//        overridedSharedUserSessionCache.close()
//        overridedTestClass.close()
//    }
}
