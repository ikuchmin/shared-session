package ru.udya.sharedsession.redis.tool;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.UuidProvider;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserSessionPermissionRepository;
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;

import java.util.UUID;

import static ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository.*;

@Component("ss_RedisSharedUserSessionRepositoryTool")
public class RedisSharedUserSessionIdTool {

    public String createSharedUserSessionId(UUID userId, UUID sessionId) {
        return String.format(KEY_PATTERN, userId, sessionId);
    }

    public String createSharedUserSessionId(UserSession userSession) {
        return createSharedUserSessionId(userSession.getUser().getId(), userSession.getId());
    }

    public UUID extractCubaUserSessionIdFromSharedUserSessionId(RedisSharedUserSessionId sharedUserSessionId) {
        var sessionKeyParts = sharedUserSessionId.getSharedId().split(RedisSharedUserSessionRepository.DELIMITER);
        return UuidProvider.fromString(sessionKeyParts[3]);
    }

    public String createSharedUserSessionRedisCommonKey(RedisSharedUserSessionId sharedUserSession) {
        return sharedUserSession.getSharedId() + ":" + COMMON_SUFFIX;
    }

    public String subtractCommonSuffix(String foundKey) {
        return foundKey.substring(0, (foundKey.indexOf(COMMON_SUFFIX) - 1));
    }

    public String createSharedUserSessionRedisCommonMatcher(Id<User, UUID> userId) {
        return KEY_PREFIX + ":" + userId.getValue() + ":*:" + COMMON_SUFFIX;
    }

    public String createSharedUserSessionRedisCommonMatcherByCubaUserSession(UUID cubaUserSessionId) {
        return KEY_PREFIX + ":*:" + cubaUserSessionId + ":" + COMMON_SUFFIX;
    }

    public String createSharedUserSessionRedisPermissionKey(RedisSharedUserSessionId sharedUserSession) {
        return sharedUserSession.getSharedId() + ":" + RedisSharedUserSessionPermissionRepository.PERMISSION_SUFFIX;
    }

    public String createSharedUserSessionRedisPermissionKey(String commonKey) {
        return subtractCommonSuffix(commonKey) + ":" + RedisSharedUserSessionPermissionRepository.PERMISSION_SUFFIX;
    }
}
