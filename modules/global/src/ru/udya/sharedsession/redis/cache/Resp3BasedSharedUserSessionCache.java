package ru.udya.sharedsession.redis.cache;

import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Resp3BasedSharedUserSessionCache implements RedisSharedUserSessionCache {

    private static final Logger log = LoggerFactory.getLogger(Resp3BasedSharedUserSessionCache.class);

    protected RedisSharedUserSessionIdTool redisSharedUserSessionIdTool;

    protected Map<String, UserSession> cache = new ConcurrentHashMap<>(10000);
    protected CacheFrontend<String, UserSession> clientSideRedisCache;

    protected StatefulRedisPubSubConnection<String, String> invalidateConnection;

    public Resp3BasedSharedUserSessionCache(StatefulRedisConnection<String, UserSession> redisConnection,
                                            RedisSharedUserSessionIdTool redisSharedUserSessionIdTool) {

        this.redisSharedUserSessionIdTool = redisSharedUserSessionIdTool;

        this.clientSideRedisCache = ClientSideCaching.enable(CacheAccessor.forMap(cache), redisConnection,
                TrackingArgs.Builder.enabled().noloop());
//                        .bcast().prefixes(RedisSharedUserSessionRepository.KEY_PREFIX)); // fallback

        // add logging
        ((ClientSideCaching<String, UserSession>) clientSideRedisCache).addInvalidationListener(
                k -> log.info("Invalidate key: {}", k));
    }

    @Nullable
    @Override
    public RedisSharedUserSession getFromCacheBySharedId(RedisSharedUserSessionId sharedUserSessionId) {
        var commonKey = redisSharedUserSessionIdTool
                .createSharedUserSessionRedisCommonKey(sharedUserSessionId);

        var userSession = clientSideRedisCache.get(commonKey);

        return RedisSharedUserSession.of(sharedUserSessionId, userSession);
    }

    @Override
    public RedisSharedUserSession getFromCacheBySharedId(
            RedisSharedUserSessionId sharedUserSessionId,
            Function<RedisSharedUserSessionId, RedisSharedUserSession> getBySessionKeyId) {

        var commonKey = redisSharedUserSessionIdTool
                .createSharedUserSessionRedisCommonKey(sharedUserSessionId);

        var userSession = clientSideRedisCache.get(commonKey,
                () -> getBySessionKeyId.apply(sharedUserSessionId).getCubaUserSession());

        return RedisSharedUserSession.of(sharedUserSessionId, userSession);
    }

    @Override
    public void saveInCache(RedisSharedUserSession redisSharedUserSession) {
        var commonKey = redisSharedUserSessionIdTool.createSharedUserSessionRedisCommonKey(redisSharedUserSession);
        cache.put(commonKey, redisSharedUserSession.getCubaUserSession());
    }

    @Override
    public void removeFromCache(RedisSharedUserSessionId sessionId) {
//        cache.remove(sessionId);

        throw new UnsupportedOperationException("Not implemented");
    }

    @PreDestroy
    public void close() {
//        invalidateConnection.close();

        throw new UnsupportedOperationException("Not implemented");
    }
}
