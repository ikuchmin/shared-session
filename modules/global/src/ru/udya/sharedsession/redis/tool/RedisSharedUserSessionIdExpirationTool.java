package ru.udya.sharedsession.redis.tool;

import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.config.RedisConfig;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component("ss_RedisSharedUserSessionIdExpirationTool")
public class RedisSharedUserSessionIdExpirationTool {

    protected final GlobalConfig globalConfig;
    protected final RedisConfig redisConfig;
    protected final RedisSharedUserSessionIdTool redisRepositoryTool;

    public RedisSharedUserSessionIdExpirationTool(GlobalConfig globalConfig,
                                                  RedisConfig redisConfig,
                                                  RedisSharedUserSessionIdTool redisRepositoryTool) {
        this.globalConfig = globalConfig;
        this.redisConfig = redisConfig;
        this.redisRepositoryTool = redisRepositoryTool;
    }

    public void updatePermissionsKeyExpirationTime(RedisAsyncCommands<String, SharedUserPermission> asyncRedisCommands,
                                                   String redisKey) throws ExecutionException, InterruptedException {
        if (isConstantUserKey(redisKey)) {
            return;
        }
        asyncRedisCommands.expire(redisKey, redisConfig.getRedisSessionTimeout()).get();
    }

    public void updateCommonKeyExpirationTime(RedisAsyncCommands<String, UserSession> asyncRedisCommands,
                                              String commonKey) throws ExecutionException, InterruptedException {
        if (isConstantUserKey(commonKey)) {
            return;
        }
        var permissionsKey = redisRepositoryTool.createSharedUserSessionRedisPermissionKey(commonKey);
        Long expiration = redisConfig.getRedisSessionTimeout();
        asyncRedisCommands.expire(commonKey, expiration).get();
        asyncRedisCommands.expire(permissionsKey, expiration).get();
    }

    public void updateCommonKeyExpirationTime(RedisCommands<String, UserSession> syncRedisCommands,
                                              String commonKey) {
        if (isConstantUserKey(commonKey)) {
            return;
        }
        var permissionsKey = redisRepositoryTool.createSharedUserSessionRedisPermissionKey(commonKey);
        Long expiration = redisConfig.getRedisSessionTimeout();
        syncRedisCommands.expire(commonKey, expiration);
        syncRedisCommands.expire(permissionsKey, expiration);
    }

    public void updateIdMappingKeyExpirationTime(RedisAsyncCommands<String, RedisSharedUserSessionId> asyncRedisCommands,
                                                 String idMappingKey) throws ExecutionException, InterruptedException {
        asyncRedisCommands.expire(idMappingKey, redisConfig.getRedisSessionTimeout()).get();
    }

    private boolean isConstantUserKey(String redisKey) {
        return isAnonymousSession(redisKey) || isConstantUserSession(redisKey);
    }

    private boolean isAnonymousSession(String commonKey) {
        String userSessionId = Optional
                .ofNullable(globalConfig.getAnonymousSessionId())
                .map(UUID::toString)
                .orElse(null);
        return doesRedisKeyContainUserId(commonKey, userSessionId);
    }

    private boolean isConstantUserSession(String redisKey) {
        List<String> sessionIds = redisConfig.getConstantSessionIds();
        if (sessionIds == null) {
            return true;
        }
        return sessionIds.stream().allMatch(id -> doesRedisKeyContainUserId(redisKey, id));
    }

    private boolean doesRedisKeyContainUserId(String redisKey, String userSessionId) {
        return userSessionId != null && redisKey.contains(userSessionId);
    }
}
