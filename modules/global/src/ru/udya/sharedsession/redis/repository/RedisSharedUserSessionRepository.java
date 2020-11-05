package ru.udya.sharedsession.redis.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionNotFoundException;
import ru.udya.sharedsession.exception.SharedSessionOptimisticLockException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.redis.codec.RedisUserSessionCodec;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Component(SharedUserSessionRepository.NAME)
public class RedisSharedUserSessionRepository implements SharedUserSessionRepository<String> {

    public static final String KEY_PREFIX = "shared:session";

    // shared:session:userId:sessionId
    public static final String KEY_PATTERN = KEY_PREFIX + ":" + "%s:%s";

    protected RedisClient redisClient;
    protected RedisUserSessionCodec redisUserSessionCodec;

    protected StatefulRedisConnection<String, UserSession> asyncReadConnection;

    @Override
    public RedisSharedUserSession findById(String sharedId) {

        try {

            asyncReadConnection.async()
                               .get((String) sharedId)
                               .get();

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

        return null;
    }

    @Override
    public List<RedisSharedUserSession> findAllByUser(Id<User, UUID> userId) {
        return Collections.emptyList();
    }

    @Override
    public List<> findAllKeysByUser(Id<User, UUID> userId) {
        return null;
    }

    @Override
    public void save(SharedUserSession sharedUserSession) {

        var sharedId = sharedUserSession.getSharedId();

        try {

            asyncReadConnection.async()
                               .set(sharedId, sharedUserSession.getCubaUserSession());

        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }


    public RedisSharedUserSession updateByFn(RedisSharedUserSessionId redisSharedUserSessionId,
                                             Consumer<RedisSharedUserSession> updateFn) {

        try (StatefulRedisConnection<String, UserSession> writeConnection =
                     redisClient.connect(redisUserSessionCodec)) {

            RedisCommands<String, UserSession> sync = writeConnection.sync();

            sync.watch(redisSharedUserSessionId.getSharedId());

            UserSession sessionFromRedis = sync.get(redisSharedUserSessionId.getSharedId());
            if (sessionFromRedis == null) {
                throw new SharedSessionNotFoundException(String.format("Session isn't found in Redis storage (Key: %s)", redisSharedUserSessionId));
            }

            var updateSharedUserSession = RedisSharedUserSession
                    .of(redisSharedUserSessionId, sessionFromRedis);

            // apply setter
            updateFn.accept(updateSharedUserSession);

            sync.multi();
            sync.set(redisSharedUserSessionId.getSharedId(), sessionFromRedis);

            TransactionResult transactionResult = sync.exec();
            if (transactionResult.wasDiscarded()) {

                throw new SharedSessionOptimisticLockException(
                        "Session changes can't be saved to Redis because someone changes session in Redis during transaction");
            }

            return updateSharedUserSession;
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }
}
