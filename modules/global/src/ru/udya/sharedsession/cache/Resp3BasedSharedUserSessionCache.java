package ru.udya.sharedsession.cache;

import com.haulmont.cuba.security.global.UserSession;
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
import ru.udya.sharedsession.redis.RedisSharedUserSessionRuntime;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component(SharedUserSessionCache.NAME)
public class Resp3BasedSharedUserSessionCache implements SharedUserSessionCache {

    private static final Logger log = LoggerFactory.getLogger(Resp3BasedSharedUserSessionCache.class);

    private RedisClient redisClient;

    private final Map<String, UserSession> cache = new ConcurrentHashMap<>(100);
    private StatefulRedisPubSubConnection<String, String> invalidateConnection;

    public Resp3BasedSharedUserSessionCache(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.invalidateConnection = redisClient.connectPubSub();
        RedisPubSubCommands<String, String> commands =
                this.invalidateConnection.sync();

        commands.clientTracking(TrackingArgs.Builder.enabled()
                .bcast().prefixes(RedisSharedUserSessionRuntime.KEY_PREFIX));
        this.invalidateConnection.addListener(message -> {

            if (message.getType().equals("invalidate")) {

                //noinspection unchecked
                List<String> keysToInvalidate = (List<String>)
                        message.getContent(StringCodec.UTF8::decodeKey).get(1);

                log.info("Invalidate keys: {}", keysToInvalidate);

                keysToInvalidate.forEach(cache::remove);
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

    @Override
    @SuppressWarnings("unchecked")
    public <T extends UserSession> T getFromCacheBySessionKey(
            String sessionKey, Function<String, T> getBySessionKeyId) {

        return (T) cache.computeIfAbsent(sessionKey, getBySessionKeyId);

    }

    @Override
    public void saveInCache(String sessionKey, UserSession userSession) {
        cache.put(sessionKey, userSession);
    }

    @Override
    public void removeFromCache(String sessionKey) {
        cache.remove(sessionKey);
    }

    @PreDestroy
    public void close() {
        invalidateConnection.close();
    }
}
