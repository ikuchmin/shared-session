package ru.udya.sharedsession.redis.repository

import com.haulmont.cuba.core.entity.contracts.Id
import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.Configuration
import com.haulmont.cuba.core.global.UserSessionSource
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.entity.User
import com.haulmont.cuba.security.global.UserSession
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.config.RedisConfig
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserSessionPermissionRepository

import java.util.stream.Collectors

class RedisSharedUserSessionRepositoryImplTest extends SharedSessionIntegrationSpecification {

    RedisSharedUserSessionRepositoryImpl testClass

    UserSessionSource uss
    RedisConfig redisConfig
//    RedisSharedUserSessionId sharedUserSessionId

    void setup() {
//        def sharedUserSessionId = String.format(KEY_PATTERN, UuidProvider.createUuid(),
//                                                UuidProvider.createUuid())
//
//        this.sharedUserSessionId = RedisSharedUserSessionId.of(sharedUserSessionId)

        uss = AppBeans.get(UserSessionSource)

        testClass = AppBeans.get(RedisSharedUserSessionRepositoryImpl)

        redisConfig = AppBeans.get(Configuration.class).getConfig(RedisConfig)
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

    def "check that keys expiration in the findById method works"() {
        def usId = UuidProvider.createUuid()

        def user = new User()
        user.with {
            id = UuidProvider.createUuid()
        }

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        when:
        def redisUserSession = testClass.createByCubaUserSession(us)

        testClass.findById(redisUserSession)
        def firstCommonTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisCommonKey(redisUserSession))
                .get()

        def firstPermissionsTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisPermissionKey(redisUserSession))
                .get()

        def delay = 5
        Thread.sleep(delay * 1000)

        testClass.findById(redisUserSession)
        def secondCommonTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisCommonKey(redisUserSession))
                .get()
        def secondPermissionsTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisPermissionKey(redisUserSession))
                .get()
        then:
        def deltaCommon = firstCommonTtl - secondCommonTtl
        deltaCommon == 0 || deltaCommon > delay

        def deltaPermissions = firstPermissionsTtl - secondPermissionsTtl
        deltaPermissions == 0 || deltaPermissions > delay

        when:
        testClass.findById(redisUserSession)

        Thread.sleep(redisConfig.getRedisSessionTimeout() * 1000)

        def foundKeyAfterTimeout = testClass
                .asyncRedisCommands
                .get(redisUserSession.sharedId)
                .get()

        def foundPermissionsKeyAfterTimeout = testClass
                .asyncRedisCommands
                .get(testClass.redisRepositoryTool.createSharedUserSessionRedisPermissionKey(redisUserSession))
                .get()

        then:
        foundKeyAfterTimeout == null
        foundPermissionsKeyAfterTimeout == null
    }

    def "check that keys expiration in the findIdByCubaUserSessionId method works"() {
        def usId = UuidProvider.createUuid()

        def user = new User()
        user.with {
            id = UuidProvider.createUuid()
        }

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        when:
        def redisUserSession = testClass.createByCubaUserSession(us)

        testClass.findIdByCubaUserSessionId(us.id)
        def firstTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisCommonKey(redisUserSession))
                .get()
        def firstPermissionsTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisPermissionKey(redisUserSession))
                .get()
        def delay = 5
        Thread.sleep(delay * 1000)

        testClass.findIdByCubaUserSessionId(us.id)
        def secondTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisCommonKey(redisUserSession))
                .get()
        def secondPermissionsTtl = testClass
                .asyncRedisCommands
                .ttl(testClass.redisRepositoryTool.createSharedUserSessionRedisPermissionKey(redisUserSession))
                .get()
        then:
        def delta = firstTtl - secondTtl
        delta == 0 || delta > delay

        def deltaPermissions = firstPermissionsTtl - secondPermissionsTtl
        deltaPermissions == 0 || deltaPermissions > delay

        when:
        testClass.findIdByCubaUserSessionId(us.id)

        Thread.sleep(redisConfig.getRedisSessionTimeout() * 1000)

        def foundKeyAfterTimeout = testClass
                .asyncRedisCommands
                .get(redisUserSession.sharedId)
                .get()

        def foundPermissionsKeyAfterTimeout = testClass
                .asyncRedisCommands
                .get(redisUserSession.sharedId)
                .get()

        then:
        foundKeyAfterTimeout == null
        foundPermissionsKeyAfterTimeout == null
    }

    def "check that keys expiration in the findAllIdsByUser method works"() {
        def usId = UuidProvider.createUuid()

        def user = new User()
        user.with {
            id = UuidProvider.createUuid()
        }

        // doesn't use default test user session because it has some hacks
        def us = new UserSession(usId, user, [], uss.getUserSession().getLocale(), false)

        when:
        def redisUserSession = testClass.createByCubaUserSession(us)

        def firstTtls = findAllTtlCommonKeysByUser(us)

        def firstPermissionsTtls = findAllTtlPermissionsKeysByUser(us)

        def delay = 5
        Thread.sleep(delay * 1000)

        def secondTtls = findAllTtlCommonKeysByUser(us)
        def secondPermissionsTtls = findAllTtlPermissionsKeysByUser(us)

        then:
        firstTtls.stream().forEach({ f1 ->
            secondTtls.forEach({ f2 ->
                def delta = f2 - f1
                delta == 0 || delta > delay
            })
        })
        firstPermissionsTtls.stream().forEach({ f1 ->
            secondPermissionsTtls.forEach({ f2 ->
                def delta = f2 - f1
                delta == 0 || delta > delay
            })
        })

        when:
        testClass.findAllIdsByUser(Id.of(us.id, User.class))

        Thread.sleep(redisConfig.getRedisSessionTimeout() * 1000)

        def foundKeyAfterTimeout = testClass
                .asyncRedisCommands
                .get(redisUserSession.sharedId)
                .get()
        def foundPermissionsKeyAfterTimeout = testClass
                .asyncRedisCommands
                .get(redisUserSession.sharedId)
                .get()

        then:
        foundKeyAfterTimeout == null
        foundPermissionsKeyAfterTimeout == null
    }

    private List<Long> findAllTtlCommonKeysByUser(UserSession us) {
        testClass.findAllIdsByUser(Id.of(us.id, User.class))
        def matcher = testClass.redisRepositoryTool.createSharedUserSessionRedisCommonMatcher(Id.of(us.id, User.class))
        def foundKeys = testClass.asyncRedisCommands.keys(matcher).get()
        return foundKeys
                .stream()
                .map({ testClass.asyncRedisCommands.ttl(it).get() })
                .collect(Collectors.toList())
    }

    private List<Long> findAllTtlPermissionsKeysByUser(UserSession us) {
        testClass.findAllIdsByUser(Id.of(us.id, User.class))
        def commonMatcher = testClass.redisRepositoryTool.createSharedUserSessionRedisCommonMatcher(Id.of(us.id, User.class))
        def matcher = testClass.redisRepositoryTool.subtractCommonSuffix(commonMatcher) + ":" + RedisSharedUserSessionPermissionRepository.PERMISSION_SUFFIX;

        def foundKeys = testClass.asyncRedisCommands.keys(matcher).get()
        return foundKeys
                .stream()
                .map({ testClass.asyncRedisCommands.ttl(it).get() })
                .collect(Collectors.toList())
    }
}
