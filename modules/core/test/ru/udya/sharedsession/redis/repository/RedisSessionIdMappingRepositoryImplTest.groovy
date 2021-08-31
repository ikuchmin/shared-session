package ru.udya.sharedsession.redis.repository

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UuidProvider
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool

class RedisSessionIdMappingRepositoryImplTest extends SharedSessionIntegrationSpecification {

    RedisSessionIdMappingRepositoryImpl testClass
    RedisSharedUserSessionIdTool redisSharedUserSessionIdTool

    void setup() {
        testClass = AppBeans.get(RedisSessionIdMappingRepositoryImpl)

        redisSharedUserSessionIdTool = AppBeans.get(RedisSharedUserSessionIdTool)
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
}
