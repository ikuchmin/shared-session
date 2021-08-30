package ru.udya.sharedsession.redis.repository

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UserSessionSource
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.global.UserSession
import io.lettuce.core.RedisClient
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.redis.cache.RedisSharedUserSessionCache
import ru.udya.sharedsession.redis.codec.RedisUserSessionCodec
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool

class RedisSharedUserSessionRepositoryCachedTest extends SharedSessionIntegrationSpecification {

    UserSessionSource uss

    RedisSharedUserSessionCache redisSharedUserSessionCache
    RedisSharedUserSessionRepositoryImpl redisSharedUserSessionRepositoryImpl

    RedisSharedUserSessionRepositoryCached testClass

    @Override
    void setup() {
        uss = AppBeans.get(UserSessionSource)

        //noinspection GroovyAssignabilityCheck
        redisSharedUserSessionRepositoryImpl = Spy(RedisSharedUserSessionRepositoryImpl,
                                                   constructorArgs: [AppBeans.get(RedisClient),
                                                                     AppBeans.get(RedisUserSessionCodec),
                                                                     AppBeans.get(RedisSharedUserSessionIdTool)])
        redisSharedUserSessionRepositoryImpl.init()

        RedisSessionIdMappingRepositoryImpl idMappingRepository = AppBeans.get(RedisSessionIdMappingRepositoryImpl)
        testClass = new RedisSharedUserSessionRepositoryCached(AppBeans.get(RedisSharedUserSessionIdTool), redisSharedUserSessionRepositoryImpl, idMappingRepository)
    }

    def "check that findById doesn't produce query to Redis every time"() {
        given:
        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        def sharedUserSession = testClass.createByCubaUserSession(us)

        when:
        testClass.findById(sharedUserSession)

        then:
        1 * redisSharedUserSessionRepositoryImpl.findById(_ as RedisSharedUserSessionId)

        when: "second find doesn't produce real execution"
        testClass.findById(sharedUserSession)

        then:
        0 * redisSharedUserSessionRepositoryImpl.findById(_ as RedisSharedUserSessionId)
    }

    def "check that saving user session invalidate cache"() {
        given:
        def us = new UserSession(UuidProvider.createUuid(), uss.getUserSession().getUser(),
                [], uss.getUserSession().getLocale(), false) // doesn't use default test user session because it has some hacks

        def sharedUserSession = testClass.createByCubaUserSession(us)

        when:
        testClass.findById(sharedUserSession)

        then:
        1 * redisSharedUserSessionRepositoryImpl.findById(_ as RedisSharedUserSessionId)

        when:
        testClass.save(sharedUserSession)

        sleep(2000)

        testClass.findById(sharedUserSession)

        then:
        1 * redisSharedUserSessionRepositoryImpl.findById(_ as RedisSharedUserSessionId)
        _ * redisSharedUserSessionRepositoryImpl._()
    }
}
