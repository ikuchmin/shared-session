package ru.udya.sharedsession.redis.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.cache.SharedUserSessionCache;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component(SharedUserSessionCache.NAME)
public class Resp3BasedSharedUserSessionCache implements RedisSharedUserSessionCache {

    private static final Logger log = LoggerFactory.getLogger(Resp3BasedSharedUserSessionCache.class);

    protected RedisClient redisClient;
    protected RedisSharedUserSessionIdTool redisSharedUserSessionIdTool;

    protected Map<RedisSharedUserSessionId, RedisSharedUserSession> cache = new ConcurrentHashMap<>(3000);
    protected StatefulRedisPubSubConnection<String, String> invalidateConnection;

    public Resp3BasedSharedUserSessionCache(RedisClient redisClient,
                                            RedisSharedUserSessionIdTool redisSharedUserSessionIdTool) {
        this.redisClient = redisClient;
        this.redisSharedUserSessionIdTool = redisSharedUserSessionIdTool;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.invalidateConnection = redisClient.connectPubSub();
        RedisPubSubCommands<String, String> commands =
                this.invalidateConnection.sync();

        commands.clientTracking(TrackingArgs.Builder.enabled()
                .bcast().prefixes(RedisSharedUserSessionRepository.KEY_PREFIX));

        this.invalidateConnection.addListener(message -> {

            if (message.getType().equals("invalidate")) {

                //noinspection unchecked
                List<String> keysToInvalidate = (List<String>)
                         message.getContent(StringCodec.UTF8::decodeKey).get(1);

                log.info("Invalidate keys: {}", keysToInvalidate);

                keysToInvalidate
                        .stream()
                        .filter(k -> k.contains(RedisSharedUserSessionRepository.COMMON_SUFFIX))
                        .map(redisSharedUserSessionIdTool::subtractCommonSuffix)
                        .map(RedisSharedUserSessionId::of)
                        .forEach(cache::remove);
            }
        });

        // flush cache if connection is lost
        EventBus eventBus = redisClient.getResources().eventBus();
        eventBus.get().subscribe(event -> {
            if (event instanceof DisconnectedEvent ||
                    event instanceof ReconnectFailedEvent) {

                cache.clear();
            }
        });
    }

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
        return cache.get(sharedUserSessionId);
    }

    @Override
    public RedisSharedUserSession getFromCacheBySharedId(
            RedisSharedUserSessionId sessionId,
            Function<RedisSharedUserSessionId, RedisSharedUserSession> getBySessionKeyId) {

       return cache.computeIfAbsent(sessionId, getBySessionKeyId);
    }

    @Override
    public void saveInCache(RedisSharedUserSession redisSharedUserSession) {
        cache.put(RedisSharedUserSessionId.of(redisSharedUserSession), redisSharedUserSession);
    }

    @Override
    public void removeFromCache(RedisSharedUserSessionId sessionId) {
        cache.remove(sessionId);
    }

    @PreDestroy
    public void close() {
        invalidateConnection.close();
    }
}
