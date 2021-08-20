package ru.udya.sharedsession.redis.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.redis.cache.RedisSharedUserSessionCache;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@Primary
@Component(SharedUserSessionRepository.NAME)
public class RedisSharedUserSessionRepositoryCached
        implements RedisSharedUserSessionRepository {

    protected static final Logger log = LoggerFactory.getLogger(RedisSharedUserSessionRepositoryCached.class);
    protected Map<UUID, RedisSharedUserSessionId> cubaSessionIdOnSharedUserSessionIdCache = new ConcurrentHashMap<>(3000);

    protected RedisSharedUserSessionCache redisSharedUserSessionCache;
    protected RedisSharedUserSessionRepositoryImpl redisSharedUserSessionRepositoryImpl;

    public RedisSharedUserSessionRepositoryCached(RedisSharedUserSessionCache redisSharedUserSessionCache,
                                                  RedisSharedUserSessionRepositoryImpl redisSharedUserSessionRepositoryImpl) {

        this.redisSharedUserSessionCache = redisSharedUserSessionCache;
        this.redisSharedUserSessionRepositoryImpl = redisSharedUserSessionRepositoryImpl;
    }

    @Override
    public RedisSharedUserSession findById(RedisSharedUserSessionId sharedId) {
        return redisSharedUserSessionCache.getFromCacheBySharedId(sharedId,
                logSessionCacheMiss(redisSharedUserSessionRepositoryImpl::findById));
    }

    @Override
    public RedisSharedUserSessionId findIdByCubaUserSessionId(UUID cubaUserSessionId) {
        return cubaSessionIdOnSharedUserSessionIdCache.computeIfAbsent(cubaUserSessionId,
                logSessionIdCacheMiss(redisSharedUserSessionRepositoryImpl::findIdByCubaUserSessionId));
    }

    @Override
    public List<RedisSharedUserSessionId> findAllIdsByUser(Id<User, UUID> userId) {
        return redisSharedUserSessionRepositoryImpl.findAllIdsByUser(userId);
    }

    @Override
    public RedisSharedUserSession createByCubaUserSession(UserSession cubaUserSession) {
        var sharedUserSession = redisSharedUserSessionRepositoryImpl.createByCubaUserSession(cubaUserSession);

        cubaSessionIdOnSharedUserSessionIdCache.put(cubaUserSession.getId(), sharedUserSession);

        return sharedUserSession;
    }

    @Override
    public void save(RedisSharedUserSession sharedUserSession) {
        redisSharedUserSessionRepositoryImpl.save(sharedUserSession);
    }

    @Override
    public RedisSharedUserSession updateByFn(RedisSharedUserSessionId redisSharedUserSessionId,
                                             Consumer<RedisSharedUserSession> updateFn) {
        return redisSharedUserSessionRepositoryImpl.updateByFn(redisSharedUserSessionId, updateFn);
    }

    public <K, V> Function<K, V> logSessionIdCacheMiss(Function<K, V> originFunction) {
        return k -> {
            log.warn("CUBA Session id isn't in the session cache. CUBA SessionId: {}", k);

            return originFunction.apply(k);
        };
    }

    public <K, V> Function<K, V> logSessionCacheMiss(Function<K, V> originFunction) {
        return k -> {
            log.warn("Session isn't in the session cache. Shared SessionId: {}", k);

            return originFunction.apply(k);
        };
    }
}
