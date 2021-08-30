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
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;
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
                TrackingArgs.Builder.enabled().prefixes(RedisSharedUserSessionRepository.KEY_PREFIX).noloop());
    }

//    @PostConstruct
//    @SuppressWarnings("unused")
//    public void init() {
//        this.invalidateConnection = redisClient.connectPubSub();
//        RedisPubSubCommands<String, String> commands =
//                this.invalidateConnection.sync();
//
//        commands.clientTracking(TrackingArgs.Builder.enabled()
//                .bcast().prefixes(RedisSharedUserSessionRepository.KEY_PREFIX));
//
//        this.invalidateConnection.addListener(message -> {
//
//            if (message.getType().equals("invalidate")) {
//
//                //noinspection unchecked
//                List<String> keysToInvalidate = (List<String>)
//                         message.getContent(StringCodec.UTF8::decodeKey).get(1);
//
//                log.info("Invalidate keys: {}", keysToInvalidate);
//
//                keysToInvalidate.stream()
//                                .filter(k -> k.contains(RedisSharedUserSessionRepository.COMMON_SUFFIX))
//                                .map(redisSharedUserSessionIdTool::subtractCommonSuffix)
//                                .map(RedisSharedUserSessionId::of)
//                                .forEach(cache::remove);
//            }
//        });
//
//        // flush cache if connection is lost
//        EventBus eventBus = redisClient.getResources().eventBus();
//        eventBus.get().subscribe(event -> {
//            if (event instanceof DisconnectedEvent ||
//                    event instanceof ReconnectFailedEvent) {
//
//                cache.clear();
//            }
//        });
//    }

//    @Override
//    @SuppressWarnings("unchecked")
//    public <T extends UserSession> T getFromCacheBySharedId(
//            String sessionKey, Function<String, T> getBySessionKeyId) {
//
//        return (T) cache.computeIfAbsent(sessionKey, getBySessionKeyId);
//
//    }
//
//    @Override
//    public void saveInCache(String sessionKey, UserSession userSession) {
//        cache.put(sessionKey, userSession);
//    }

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
//        cache.put(RedisSharedUserSessionId.of(redisSharedUserSession), redisSharedUserSession);
        throw new UnsupportedOperationException("Not implemented");
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
