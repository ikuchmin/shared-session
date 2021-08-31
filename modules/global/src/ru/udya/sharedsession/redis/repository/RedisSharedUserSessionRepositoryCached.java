package ru.udya.sharedsession.redis.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.redis.cache.RedisSharedUserSessionCache;
import ru.udya.sharedsession.redis.cache.Resp3BasedSharedUserSessionCache;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import javax.annotation.PostConstruct;
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

    protected RedisSharedUserSessionIdTool redisSharedUserSessionIdTool;
    protected RedisSharedUserSessionRepositoryImpl redisSharedUserSessionRepositoryImpl;
    protected RedisSessionIdMappingRepository redisCubaUserSessionIdOnSharedUserSessionIdMappingRepository;

    protected RedisSharedUserSessionCache redisSharedUserSessionCache;

    public RedisSharedUserSessionRepositoryCached(RedisSharedUserSessionIdTool redisSharedUserSessionIdTool,
                                                  RedisSharedUserSessionRepositoryImpl redisSharedUserSessionRepositoryImpl,
                                                  RedisSessionIdMappingRepository redisCubaUserSessionIdOnSharedUserSessionIdMappingRepository) {
        this.redisSharedUserSessionIdTool = redisSharedUserSessionIdTool;

        this.redisSharedUserSessionRepositoryImpl = redisSharedUserSessionRepositoryImpl;
        this.redisCubaUserSessionIdOnSharedUserSessionIdMappingRepository = redisCubaUserSessionIdOnSharedUserSessionIdMappingRepository;
    }

    @PostConstruct
    public void init() {
        this.redisSharedUserSessionCache = new Resp3BasedSharedUserSessionCache(
                redisSharedUserSessionRepositoryImpl.getRedisConnection(), redisSharedUserSessionIdTool);

    }

    @Override
    public RedisSharedUserSession findById(RedisSharedUserSessionId sharedId) {
        return redisSharedUserSessionCache.getFromCacheBySharedId(sharedId,
                logSessionCacheMiss(redisSharedUserSessionRepositoryImpl::findById));
    }

    @Override
    public RedisSharedUserSessionId findIdByCubaUserSessionId(UUID cubaUserSessionId) {

        // composite fast and slow version of finding sharedSession
        Function<UUID, RedisSharedUserSessionId> cachedFindIdByCubaUserSessionId = (UUID cubaId) -> {
            var sharedUserSessionId = redisCubaUserSessionIdOnSharedUserSessionIdMappingRepository
                    .findSharedIdByCubaSessionId(cubaId);

            if (sharedUserSessionId != null) {

                // check that mapping in redis isn't corrupted
                if (sharedUserSessionId.getSharedId().endsWith(cubaId.toString())) {

                    log.info("Redis cache hit for CUBA session ID ({}) -> {}",
                            cubaUserSessionId, sharedUserSessionId.getSharedId());

                    return sharedUserSessionId;
                }

                log.warn("Cache miss for cubaId on sharedId mapping. SharedId ({}) isn't consist with cubaId ({})",
                        sharedUserSessionId.getSharedId(), cubaId);
            }

            log.info("Go slow fallback for {}", cubaUserSessionId);

            // slow fallback
            return  redisSharedUserSessionRepositoryImpl.findIdByCubaUserSessionId(cubaId);
        };

        // todo avoid local cache during the moment when we guarantee that sessionId is uniq (using setnx in repo)
        return cachedFindIdByCubaUserSessionId.apply(cubaUserSessionId);

        // var redisSharedUserSessionId = cubaSessionIdOnSharedUserSessionIdCache.computeIfAbsent(cubaUserSessionId,
        //        logSessionIdCacheMiss(findIdByCubaUserSessionId));

        // return redisSharedUserSessionId;
    }

    @Override
    public List<RedisSharedUserSessionId> findAllIdsByUser(Id<User, UUID> userId) {
        return redisSharedUserSessionRepositoryImpl.findAllIdsByUser(userId);
    }

    @Override
    public RedisSharedUserSession createByCubaUserSession(UserSession cubaUserSession) {
        var sharedUserSession = redisSharedUserSessionRepositoryImpl.createByCubaUserSession(cubaUserSession);

        // local cache for mapping
        // todo avoid local cache during the moment when we guarantee that sessionId is uniq (using setnx in repo)
        // cubaSessionIdOnSharedUserSessionIdCache.put(cubaUserSession.getId(), sharedUserSession);

        // redis cache for mapping
        redisCubaUserSessionIdOnSharedUserSessionIdMappingRepository
                .createSessionIdMapping(cubaUserSession.getId(), sharedUserSession);

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
