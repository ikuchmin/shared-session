package ru.udya.sharedsession.repository;

import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.role.RoleDefinition;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import ru.udya.sharedsession.cache.SharedUserSessionCache;
import ru.udya.sharedsession.redis.RedisSharedUserSession;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Inspired by {@link org.springframework.session.SessionRepository}
 */
public interface SharedUserSessionRepository {

    String NAME = "ss_SharedUserSessionRepository";

    default UserSession createSession(UUID sessionId, User user,
                                      Collection<RoleDefinition> roles,
                                      Locale locale, boolean system) {
        return createSession(new UserSession(sessionId, user, roles, locale, system));
    }

    default UserSession createSession(UserSession src, User user,
                                      Collection<RoleDefinition> roles,
                                      Locale locale) {
        return createSession(new UserSession(src, user, roles, locale));
    }

    UserSession createSession(UserSession src);

    void save(UserSession session);

    UserSession findById(UUID id);

    List<UserSession> findAllUserSessions();

    void delete(UserSession session);

    void deleteById(UserSession session);

    void saveInCache(String sessionKey, RedisSharedUserSession sharedUserSession);

    RedisClient getRedisClient();

    RedisCodec<String, UserSession> getObjectRedisCodec();

    StatefulRedisConnection<String, UserSession> getAsyncReadConnection();

    SharedUserSessionCache getSessionCache();

    RedisSharedUserSession findBySessionKeyNoCache(String sessionKey);
}
