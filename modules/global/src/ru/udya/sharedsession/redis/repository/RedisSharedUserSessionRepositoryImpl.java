package ru.udya.sharedsession.redis.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.core.global.UuidProvider;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionNotFoundException;
import ru.udya.sharedsession.exception.SharedSessionOptimisticLockException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.redis.codec.RedisUserSessionCodec;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Component("ss_RedisSharedUserSessionRepositoryImpl")
public class RedisSharedUserSessionRepositoryImpl
        implements RedisSharedUserSessionRepository {

    protected RedisClient redisClient;
    protected RedisUserSessionCodec redisUserSessionCodec;

    protected StatefulRedisConnection<String, UserSession> asyncReadConnection;

    public RedisSharedUserSessionRepositoryImpl(RedisClient redisClient,
                                                RedisUserSessionCodec redisUserSessionCodec) {
        this.redisClient = redisClient;
        this.redisUserSessionCodec = redisUserSessionCodec;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.asyncReadConnection = redisClient.connect(redisUserSessionCodec);
    }


    @PreDestroy
    public void close() {
        asyncReadConnection.close();
    }

    @Override
    public RedisSharedUserSession findById(RedisSharedUserSessionId sharedUserSessionId) {

        try {

            var sharedId = sharedUserSessionId.getSharedId();

            asyncReadConnection.async()
                               .get(sharedId)
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
    public List<RedisSharedUserSessionId> findAllKeysByUser(Id<User, UUID> userId) {
        return Collections.emptyList();
    }

    @Override
    public List<RedisSharedUserSessionId> findAllKeysByUsers(Ids<User, UUID> userId) {
        return null;
    }

    @Override
    public RedisSharedUserSession createByCubaUserSession(UserSession cubaUserSession) {
        var sharedUserSession = RedisSharedUserSession
                .of(createSharedUserSessionId(cubaUserSession), cubaUserSession);

        save(sharedUserSession);

        return sharedUserSession;
    }

    @Override
    public void save(RedisSharedUserSession sharedUserSession) {

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


    @Override
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

    // don't use Id<> because it is internal API
    protected String createSharedUserSessionId(UUID userId, UUID sessionId) {
        return String.format(KEY_PATTERN, userId, sessionId);
    }

    protected String createSharedUserSessionId(UserSession userSession) {
        return createSharedUserSessionId(userSession.getUser().getId(), userSession.getId());
    }

    protected UUID extractUserSessionIdFromSharedUserSessionKey(String sharedUserSessionKey) {
        var sessionKeyParts = sharedUserSessionKey.split(":");
        return UuidProvider.fromString(sessionKeyParts[3]);
    }

}
