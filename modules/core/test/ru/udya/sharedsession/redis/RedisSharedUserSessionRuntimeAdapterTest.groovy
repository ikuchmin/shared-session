package ru.udya.sharedsession.redis

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UserSessionSource
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.global.UserSession
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.cache.SharedUserSessionCache
import spock.lang.Ignore

class RedisSharedUserSessionRuntimeAdapterTest extends SharedSessionIntegrationSpecification {
    
    RedisSharedUserSessionRuntimeAdapter testClass

    UserSessionSource uss

    SharedUserSessionCache sharedUserSessionCache

    void setup() {
        uss = AppBeans.get(UserSessionSource)
        sharedUserSessionCache = AppBeans.get(SharedUserSessionCache)

        testClass = AppBeans.get(RedisSharedUserSessionRuntimeAdapter)
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
        redisUserSession.getSharedId() == "shared:session:$user.id:$usId"
    }

    def "check that finding shared user session works as well"() {
        given:
        def usId = UuidProvider.createUuid()
        def user = uss.getUserSession().getUser()

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        def createdSharedSession = testClass.createSession(us)

        when:
        def redisSharedSession = testClass.findBySharedId(createdSharedSession.sharedId)

        then:
        redisSharedSession.getLocale() == us.locale
    }

    @Ignore
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
//                AppBeans.get(RedisSharedUserSessionPermissionRuntime),
//                AppBeans.get(RedisSharedUserSessionPermissionRepository))
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
