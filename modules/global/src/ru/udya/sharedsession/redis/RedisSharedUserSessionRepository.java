package ru.udya.sharedsession.redis;

import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.cache.SharedUserSessionCache;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.redis.codec.RedisUserSessionCodec;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component(SharedUserSessionRepository.NAME)
public class RedisSharedUserSessionRepository
        implements SharedUserSessionRepository {

    public static final String KEY_PREFIX = "shared:session";

    protected SharedUserSessionCache sessionCache;

    protected RedisClient redisClient;
    protected RedisCodec<String, UserSession> objectRedisCodec;
    protected StatefulRedisConnection<String, UserSession> asyncReadConnection;

    public RedisSharedUserSessionRepository(RedisClient redisClient,
                                            SharedUserSessionCache sessionCache) {
        this.redisClient = redisClient;
        this.sessionCache = sessionCache;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.objectRedisCodec = RedisUserSessionCodec.INSTANCE;
        this.asyncReadConnection = redisClient.connect(objectRedisCodec);
    }

    public RedisClient getRedisClient() {
        return redisClient;
    }

    public RedisCodec<String, UserSession> getObjectRedisCodec() {
        return objectRedisCodec;
    }

    public StatefulRedisConnection<String, UserSession> getAsyncReadConnection() {
        return asyncReadConnection;
    }

    public SharedUserSessionCache getSessionCache() {
        return sessionCache;
    }

    @Override
    public UserSession createSession(UserSession src) {
        RedisSharedUserSession sharedUserSession
                = new RedisSharedUserSession(src);

        // save session in redis during creation
        sharedUserSession.save();

        return sharedUserSession;
    }

    @Override
    public void save(UserSession session) {
        if (session instanceof RedisSharedUserSession) {
            // everything is saved if it is redis session
            return;
        }

        createSession(session);
    }

    @Override
    public UserSession findById(UUID id) {
        // todo check that session is exist
        return new RedisSharedUserSession(id);
    }

    @Override
    public List<UserSession> findAllUserSessions() {
        // todo implement
        return Collections.emptyList();
    }

    @Override
    public void delete(UserSession id) {
        // todo implement
        throw new NotImplementedException("Will be implemented in a future");
    }

    @Override
    public void deleteById(UserSession session) {
        // todo implement
        throw new NotImplementedException("Will be implemented in a future");
    }

    @Override
    public void saveInCache(String sessionKey, RedisSharedUserSession sharedUserSession) {
        sessionCache.saveInCache(sessionKey, sharedUserSession);
    }

    @PreDestroy
    public void close() {
        asyncReadConnection.close();
    }

    public RedisSharedUserSession findBySessionKeyNoCache(String sessionKey) {
        try {

            UserSession userSession = asyncReadConnection.async()
                    .get(sessionKey).get();

            return new RedisSharedUserSession(userSession, sessionKey);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during getting user session", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during getting user session", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }
}
