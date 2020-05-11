package ru.udya.sharedsession.redis;

import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.RedisClient;
import ru.udya.sharedsession.cache.CoreSharedUserSessionRequestScopeCache;
import ru.udya.sharedsession.config.RedisConfig;
import ru.udya.sharedsession.exception.SharedSessionOptimisticLockException;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class CachedRedisSharedUserSessionRepository
        extends RedisSharedUserSessionRepository
        implements SharedUserSessionRepository {

    protected CoreSharedUserSessionRequestScopeCache sessionRequestScopeCoreCache;

    public CachedRedisSharedUserSessionRepository(RedisConfig redisConfig, RedisClient redisClient,
                                                  CoreSharedUserSessionRequestScopeCache sessionRequestScopeCoreCache) {
        super(redisConfig, redisClient);
        this.sessionRequestScopeCoreCache = sessionRequestScopeCoreCache;
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

        @Override
        protected void save() {
            super.save();

            sessionRequestScopeCoreCache.saveUserSessionInCache(this);
        }

        @Override
        protected UserSession safeUpdatingValue(Consumer<RedisSharedUserSession> updateFn) {
            try {
                UserSession updatedUserSession = super.safeUpdatingValue(updateFn);

                sessionRequestScopeCoreCache.saveUserSessionInCache(updatedUserSession);

                return updatedUserSession;
            } catch (SharedSessionOptimisticLockException e) {
                sessionRequestScopeCoreCache.removeUserSessionFromCache(this);
                throw e;
            }
        }

        @Override
        protected <T> T safeGettingValue(Function<RedisSharedUserSession, T> getter) {

            RedisSharedUserSession sharedUserSessionFromRedis = sessionRequestScopeCoreCache
                    .getUserSessionFromCache(this.id, this.sessionKey);

            if (sharedUserSessionFromRedis == null) {
                sharedUserSessionFromRedis = getSharedUserSessionFromRedis(sessionKey);

                sessionRequestScopeCoreCache.saveUserSessionInCache(sharedUserSessionFromRedis);
            }

            return getter.apply(sharedUserSessionFromRedis);
        }
    }


}
