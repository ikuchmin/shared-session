package ru.udya.sharedsession.redis.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
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
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserSessionPermissionRepositoryImpl;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdExpirationTool;
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
    protected RedisSharedUserSessionIdExpirationTool expirationTool;
    protected GlobalConfig globalConfig;
    protected RedisSharedUserSessionPermissionRepositoryImpl redisSharedUserPermissionRepositoryImpl;

    protected StatefulRedisConnection<String, UserSession> redisConnection;
    protected RedisAsyncCommands<String, UserSession> asyncRedisCommands;

    public RedisSharedUserSessionRepositoryImpl(RedisClient redisClient,
                                                RedisUserSessionCodec redisUserSessionCodec,
                                                RedisSharedUserSessionIdTool redisRepositoryTool,
                                                RedisSharedUserSessionIdExpirationTool expirationTool,
                                                RedisSharedUserSessionPermissionRepositoryImpl redisSharedUserPermissionRepositoryImpl,
                                                GlobalConfig globalConfig) {
        this.redisClient = redisClient;
        this.redisUserSessionCodec = redisUserSessionCodec;
        this.redisRepositoryTool = redisRepositoryTool;
        this.expirationTool = expirationTool;
        this.redisSharedUserPermissionRepositoryImpl = redisSharedUserPermissionRepositoryImpl;
        this.globalConfig = globalConfig;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.redisConnection = redisClient.connect(redisUserSessionCodec);
        this.asyncRedisCommands = redisConnection.async();
    }

    public StatefulRedisConnection<String, UserSession> getRedisConnection() {
        return redisConnection;
    }

    @PreDestroy
    public void close() {
        redisConnection.close();
    }

    @Override
    public RedisSharedUserSession findById(RedisSharedUserSessionId sharedUserSessionId) {

        try {

            var redisKey = redisRepositoryTool.createSharedUserSessionRedisCommonKey(sharedUserSessionId);

            var userSession = asyncRedisCommands.get(redisKey).get();
            updateKeyExpirationTime(redisKey);

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

            var matcher = matches(redisRepositoryTool.createSharedUserSessionRedisCommonMatcherByCubaUserSession(cubaUserSessionId));


            KeyScanCursor<String> cursor = asyncRedisCommands.scan(matcher).get();

            List<String> foundKeys = cursor.getKeys();

            while (foundKeys.isEmpty() && ! cursor.isFinished()) {

                cursor = asyncRedisCommands.scan(cursor, matcher).get();

                foundKeys = cursor.getKeys();
            }

            if (foundKeys.isEmpty()) {
                return null;
            }

            updateKeysExpirationTime(foundKeys);
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

    private void updateKeysExpirationTime(List<String> redisKeys) throws ExecutionException, InterruptedException {
        for (String key : redisKeys) {
            updateKeyExpirationTime(key);
        }
    }

    @Override
    public List<RedisSharedUserSessionId> findAllIdsByUser(Id<User, UUID> userId) {

        try {

            String matcher = redisRepositoryTool.createSharedUserSessionRedisCommonMatcher(userId);

            var foundKeys = asyncRedisCommands.keys(matcher).get();
            updateKeysExpirationTime(foundKeys);

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

            asyncRedisCommands.set(redisKey, sharedUserSession.getCubaUserSession()).get();
            updateKeyExpirationTime(redisKey);
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SharedSessionReadingException("Thread is interrupted by external process during saving shared user session", e);
        } catch (ExecutionException e) {
            throw new SharedSessionReadingException("Exception during saving shared user session", e);
        }
    }

    private void updateKeyExpirationTime(String commonKey) throws ExecutionException, InterruptedException {
        expirationTool.updateCommonKeyExpirationTime(asyncRedisCommands, commonKey);
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

            expirationTool.updateCommonKeyExpirationTime(sync, redisKey);

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

    public void updateExpirationTime(RedisSharedUserSessionId sharedUserSessionId) {
        try {
            var redisKey = redisRepositoryTool.createSharedUserSessionRedisCommonKey(sharedUserSessionId);
            updateKeyExpirationTime(redisKey);
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
}
