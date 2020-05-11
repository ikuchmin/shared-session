package ru.udya.sharedsession.redis;

import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.RedisClient;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import java.util.UUID;

public class CoreRedisSharedUserSessionRepository
        extends RedisSharedUserSessionRepository
        implements SharedUserSessionRepository {

    public CoreRedisSharedUserSessionRepository(RedisClient redisClient) {
        super(redisClient);
    }

    protected class CoreRedisSharedUserSession extends RedisSharedUserSession {

        public CoreRedisSharedUserSession(UUID id) {
            super(id);
        }

        public CoreRedisSharedUserSession(UserSession userSession) {
            super(userSession);
        }

        public CoreRedisSharedUserSession(UserSession userSession, String sessionKey) {
            super(userSession, sessionKey);
        }
    }
}
