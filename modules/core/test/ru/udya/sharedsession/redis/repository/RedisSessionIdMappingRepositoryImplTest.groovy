package ru.udya.sharedsession.redis.repository

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.Configuration
import com.haulmont.cuba.core.global.UuidProvider
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.config.RedisConfig
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool

class RedisSessionIdMappingRepositoryImplTest extends SharedSessionIntegrationSpecification {

    RedisSessionIdMappingRepositoryImpl testClass
    RedisSharedUserSessionIdTool redisSharedUserSessionIdTool
    RedisConfig redisConfig

    void setup() {
        testClass = AppBeans.get(RedisSessionIdMappingRepositoryImpl)

        redisSharedUserSessionIdTool = AppBeans.get(RedisSharedUserSessionIdTool)

        redisConfig = AppBeans.get(Configuration.class).getConfig(RedisConfig)
    }

    def "check that redis return null if cubaSessionId not found in redis"() {
        given:
        def cubaSessionId = UuidProvider.createUuid()
        def sharedSessionId = RedisSharedUserSessionId.of(redisSharedUserSessionIdTool
                .createSharedUserSessionId(UuidProvider.createUuid(), cubaSessionId))

        def anotherCubaSessionId = UuidProvider.createUuid()

        when:
        testClass.createSessionIdMapping(cubaSessionId, sharedSessionId)
        def foundSharedSessionId = testClass.findSharedIdByCubaSessionId(cubaSessionId)

        then:
        cubaSessionId != anotherCubaSessionId
        foundSharedSessionId == sharedSessionId

        when:
        foundSharedSessionId = testClass.findSharedIdByCubaSessionId(anotherCubaSessionId)

        then:
        cubaSessionId != anotherCubaSessionId
        foundSharedSessionId == null
    }

    def "check that redis session expiration works"() {
        given:
        def cubaSessionId = UuidProvider.createUuid()
        def sharedSessionId = RedisSharedUserSessionId.of(redisSharedUserSessionIdTool
                .createSharedUserSessionId(UuidProvider.createUuid(), cubaSessionId))

        testClass.createSessionIdMapping(cubaSessionId, sharedSessionId)

        when:
        testClass.findSharedIdByCubaSessionId(cubaSessionId)
        def firstTtl = testClass
                .asyncCommands
                .ttl(testClass.createSessionIdMappingKey(cubaSessionId))
                .get()
        def delay = 5
        Thread.sleep(delay * 1000)

        testClass.findSharedIdByCubaSessionId(cubaSessionId)

        def secondTtl = testClass
                .asyncCommands
                .ttl(testClass.createSessionIdMappingKey(cubaSessionId))
                .get()

        then:
        def delta = firstTtl - secondTtl
        delta == 0 || delta > delay

        when:
        testClass.createSessionIdMapping(cubaSessionId, sharedSessionId)
        def foundKey = testClass.findSharedIdByCubaSessionId(cubaSessionId)

        Thread.sleep(redisConfig.getRedisSessionTimeout() * 1000)

        def foundExpiredKey = testClass.findSharedIdByCubaSessionId(cubaSessionId)

        then:
        foundKey != null
        foundExpiredKey == null
    }
}
