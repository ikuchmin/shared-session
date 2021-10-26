package ru.udya.sharedsession.redis.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.redis.codec.RedisSessionIdMappingCodec;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdExpirationTool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Component(RedisSessionIdMappingRepositoryImpl.NAME)
public class RedisSessionIdMappingRepositoryImpl
        implements RedisSessionIdMappingRepository {

    public static final String NAME = "ss_RedisSessionIdMappingRepositoryImpl";

    protected static final Logger log = LoggerFactory.getLogger(RedisSessionIdMappingRepositoryImpl.class);

    protected RedisClient redisClient;
    protected RedisSessionIdMappingCodec redisSessionIdMappingCodec;
    protected RedisSharedUserSessionIdExpirationTool expirationTool;
    protected RedisAsyncCommands<String, RedisSharedUserSessionId> asyncCommands;

    public RedisSessionIdMappingRepositoryImpl(
            RedisClient redisClient, RedisSessionIdMappingCodec redisSessionIdMappingCodec,
            RedisSharedUserSessionIdExpirationTool expirationTool) {

        this.redisClient = redisClient;
        this.redisSessionIdMappingCodec = redisSessionIdMappingCodec;
        this.expirationTool = expirationTool;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.asyncCommands = redisClient.connect(redisSessionIdMappingCodec).async();
    }

    @PreDestroy
    public void close() {
        asyncCommands.getStatefulConnection().close();
    }

    @Override
    public RedisSharedUserSessionId findSharedIdByCubaSessionId(UUID cubaUserSessionId) {

        var fullKeyToFind = createSessionIdMappingKey(cubaUserSessionId);

        try {

            RedisFuture<RedisSharedUserSessionId> redisSharedUserSessionIdRedisFuture = asyncCommands.get(fullKeyToFind);

            updateKeyExpirationTime(fullKeyToFind);

            return redisSharedUserSessionIdRedisFuture.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during getting shared user session id by cuba user session id", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during getting shared user session id by cuba user session id", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    @Override
    public void createSessionIdMapping(UUID cubaUserSessionId, RedisSharedUserSessionId redisSharedUserSessionId) {

        var fullKeyToFind = createSessionIdMappingKey(cubaUserSessionId);

        try {

            asyncCommands.set(fullKeyToFind, redisSharedUserSessionId).get();

            updateKeyExpirationTime(fullKeyToFind);

        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during saving mapping between cuba user session id and shared user session id", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during saving mapping between cuba user session id and shared user session id", e);
        }
    }

    private void updateKeyExpirationTime(String key) throws InterruptedException, ExecutionException {
        expirationTool.updateIdMappingKeyExpirationTime(asyncCommands, key);
    }

    protected String createSessionIdMappingKey(UUID cubaUserSessionId) {
        return String.format(KEY_PATTERN, cubaUserSessionId);
    }
}
