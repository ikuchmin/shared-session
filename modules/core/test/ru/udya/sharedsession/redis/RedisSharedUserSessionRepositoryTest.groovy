package ru.udya.sharedsession.redis

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UserSessionSource
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.global.UserSession
import io.lettuce.core.RedisClient
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.cache.Resp3BasedSharedUserSessionCache
import ru.udya.sharedsession.cache.SharedUserSessionCache

class RedisSharedUserSessionRepositoryTest extends SharedSessionIntegrationSpecification {
    
    RedisSharedUserSessionRepository testClass

    UserSessionSource uss
    RedisClient redisClient
    SharedUserSessionCache sharedUserSessionCache

    void setup() {
        uss = AppBeans.get(UserSessionSource)
        redisClient = AppBeans.get(RedisClient)
        sharedUserSessionCache = AppBeans.get(SharedUserSessionCache)

        testClass = Spy(RedisSharedUserSessionRepository, constructorArgs: [redisClient, sharedUserSessionCache])
        testClass.init()
    }

    def "check that creating shared user session works as well"() {
        given:
        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        when:
        def redisUserSession = testClass.createSession(us)

        then:
        redisUserSession.getId() == us.id
    }

    def "check that finding shared user session works as well"() {
        given:
        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        testClass.createSession(us)

        when:
        def redisSharedSession = testClass.findById(us.id)

        then:
        redisSharedSession.getLocale() == us.locale
    }

    def "check that reading values from shared user session doesn't produce query to Redis every time"() {
        given:
        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        def rus = testClass.createSession(us)

        and: 'remove object from cache'
        sharedUserSessionCache.removeFromCache(rus.sessionKey)

        when:
        def redisSharedSession = testClass.findById(us.id)

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

        def rus = testClass.createSession(us)

        when:
        rus.locale = null

        then:
        testClass.findBySessionKeyNoCache(rus.sessionKey).locale == null
    }

    def "check that updating value flushing cache by invalidate message from Redis"() {
        given:
        def overridedSharedUserSessionCache = new Resp3BasedSharedUserSessionCache(redisClient) {
            @Override
            void saveInCache(String sessionKey, UserSession userSession) {
                // disable direct saving in cache
            }
        }
        overridedSharedUserSessionCache.init()

        def overridedTestClass = new RedisSharedUserSessionRepository(redisClient, overridedSharedUserSessionCache)
        overridedTestClass.init()

        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        when:
        def rus = overridedTestClass.createSession(us)

        then:
        rus.locale == us.locale

        when:
        rus.locale = null

        and: 'waiting'
        Thread.sleep(200)

        then:
        rus.locale == null

        cleanup:
        overridedSharedUserSessionCache.close()
        overridedTestClass.close()
    }
}
