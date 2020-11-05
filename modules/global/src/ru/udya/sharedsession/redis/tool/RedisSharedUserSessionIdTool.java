package ru.udya.sharedsession.redis.tool;

import com.haulmont.cuba.core.global.UuidProvider;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import java.util.UUID;

import static ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository.COMMON_SUFFIX;
import static ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository.KEY_PATTERN;

@Component("ss_RedisSharedUserSessionRepositoryTool")
public class RedisSharedUserSessionIdTool {

    public String createSharedUserSessionId(UUID userId, UUID sessionId) {
        return String.format(KEY_PATTERN, userId, sessionId);
    }

    public String createSharedUserSessionId(UserSession userSession) {
        return createSharedUserSessionId(userSession.getUser().getId(), userSession.getId());
    }

    public UUID extractUserSessionIdFromSharedUserSessionKey(String sharedUserSessionKey) {
        var sessionKeyParts = sharedUserSessionKey.split(":");
        return UuidProvider.fromString(sessionKeyParts[3]);
    }

    public String createSharedUserSessionRedisCommonKey(RedisSharedUserSessionId sharedUserSession) {
        return sharedUserSession.getSharedId() + ":" + COMMON_SUFFIX;
    }

    public String subtractCommonSuffix(String foundKey) {
        return foundKey.substring(0, (foundKey.indexOf(COMMON_SUFFIX) - 1));
    }
}
