package ru.udya.sharedsession.redis.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.KeyScanCursor;
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
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.lettuce.core.ScanArgs.Builder.matches;

@Component(RedisSharedUserSessionRepositoryImpl.NAME)
public class RedisSharedUserSessionRepositoryImpl
        implements RedisSharedUserSessionRepository {

    // don't remove it. It helps BeanLocator find proper class
    public static final String NAME = "ss_RedisSharedUserSessionRepositoryImpl";

    protected RedisClient redisClient;
    protected RedisUserSessionCodec redisUserSessionCodec;
    protected RedisSharedUserSessionIdTool redisRepositoryTool;

    protected StatefulRedisConnection<String, UserSession> asyncReadConnection;

    public RedisSharedUserSessionRepositoryImpl(RedisClient redisClient,
                                                RedisUserSessionCodec redisUserSessionCodec,
                                                RedisSharedUserSessionIdTool redisRepositoryTool) {
        this.redisClient = redisClient;
        this.redisUserSessionCodec = redisUserSessionCodec;
        this.redisRepositoryTool = redisRepositoryTool;
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

            var redisKey = redisRepositoryTool.createSharedUserSessionRedisCommonKey(sharedUserSessionId);

            var userSession = asyncReadConnection.async()
                                                 .get(redisKey)
                                                 .get();

            return RedisSharedUserSession.of(sharedUserSessionId, userSession);

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
    }

    @Nullable
    @Override
    public RedisSharedUserSessionId findIdByCubaUserSessionId(UUID cubaUserSessionId) {
        try {

            var matcher = matches(KEY_PREFIX + ":*:" + cubaUserSessionId + ":" + COMMON_SUFFIX);


            KeyScanCursor<String> cursor = asyncReadConnection.async()
                                                              .scan(matcher)
                                                              .get();
            List<String> foundKeys = cursor.getKeys();

            while (foundKeys.isEmpty() && ! cursor.isFinished()) {

                cursor = asyncReadConnection.async()
                                            .scan(cursor, matcher)
                                            .get();

                foundKeys = cursor.getKeys();
            };

            if (foundKeys.isEmpty()) {
                return null;
            }

            var foundKeyWithoutSuffix = redisRepositoryTool.subtractCommonSuffix(foundKeys.get(0));

            return RedisSharedUserSessionId.of(foundKeyWithoutSuffix);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during finding matched user session id", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during finding matched user session id", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    @Override
    public List<RedisSharedUserSessionId> findAllIdsByUser(Id<User, UUID> userId) {

        try {

            var matcher = KEY_PREFIX + ":" + userId.getValue() + ":*:" + COMMON_SUFFIX;

            var foundKeys = asyncReadConnection.async()
                                               .keys(matcher)
                                               .get();

            return foundKeys.stream()
                            .map(redisRepositoryTool::subtractCommonSuffix)
                            .map(RedisSharedUserSessionId::of)
                            .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during finding matched user session id", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during finding matched user session id", e);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    @Override
    public RedisSharedUserSession createByCubaUserSession(UserSession cubaUserSession) {
        var sharedUserSession = RedisSharedUserSession
                .of(redisRepositoryTool.createSharedUserSessionId(cubaUserSession), cubaUserSession);

        save(sharedUserSession);

        return sharedUserSession;
    }

    @Override
    public void save(RedisSharedUserSession sharedUserSession) {

        var redisKey = redisRepositoryTool.createSharedUserSessionRedisCommonKey(sharedUserSession);

        try {

            asyncReadConnection.async()
                               .set(redisKey, sharedUserSession.getCubaUserSession());

        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }


    @Override
    public RedisSharedUserSession updateByFn(RedisSharedUserSessionId redisSharedUserSessionId,
                                             Consumer<RedisSharedUserSession> updateFn) {

        var redisKey = redisRepositoryTool.createSharedUserSessionRedisCommonKey(redisSharedUserSessionId);

        try (StatefulRedisConnection<String, UserSession> writeConnection =
                     redisClient.connect(redisUserSessionCodec)) {

            RedisCommands<String, UserSession> sync = writeConnection.sync();

            sync.watch(redisKey);

            UserSession sessionFromRedis = sync.get(redisKey);
            if (sessionFromRedis == null) {
                throw new SharedSessionNotFoundException(String.format("Session isn't found in Redis storage (Key: %s)", redisSharedUserSessionId));
            }

            var updateSharedUserSession = RedisSharedUserSession
                    .of(redisSharedUserSessionId, sessionFromRedis);

            // apply setter
            updateFn.accept(updateSharedUserSession);

            sync.multi();
            sync.set(redisKey, sessionFromRedis);

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
