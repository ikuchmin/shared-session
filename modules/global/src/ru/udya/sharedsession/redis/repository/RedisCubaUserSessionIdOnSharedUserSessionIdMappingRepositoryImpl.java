package ru.udya.sharedsession.redis.repository;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.redis.codec.RedisSharedUserSessionIdCodec;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component(RedisCubaUserSessionIdOnSharedUserSessionIdMappingRepositoryImpl.NAME)
public class RedisCubaUserSessionIdOnSharedUserSessionIdMappingRepositoryImpl
        implements RedisCubaUserSessionIdOnSharedUserSessionIdMappingRepository {

    public static final String NAME = "ss_RedisCubaUserSessionIdOnSharedUserSessionIdMappingRepositoryImpl";

    protected RedisClient redisClient;
    protected RedisSharedUserSessionIdCodec sharedUserSessionIdCodec;
    protected RedisAsyncCommands<UUID, RedisSharedUserSessionId> asyncCommands;

    public RedisCubaUserSessionIdOnSharedUserSessionIdMappingRepositoryImpl(
            RedisClient redisClient, RedisSharedUserSessionIdCodec sharedUserSessionIdCodec) {

        this.redisClient = redisClient;
        this.sharedUserSessionIdCodec = sharedUserSessionIdCodec;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.asyncCommands = redisClient.connect(sharedUserSessionIdCodec).async();
    }

    @PreDestroy
    public void close() {
        asyncCommands.getStatefulConnection().close();
    }

    @Override
    public RedisSharedUserSessionId findRedisSharedUserSessionIdByCubaUserSessionId(UUID cubaUserSessionId) {

        try {

            return asyncCommands.get(cubaUserSessionId).get();

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
    public void createCubaUserSessionIdOnSharedUserSessionIdMapping(UUID cubaUserSessionId, RedisSharedUserSessionId redisSharedUserSessionId) {
        try {

            asyncCommands.set(cubaUserSessionId, redisSharedUserSessionId).get();

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
}
