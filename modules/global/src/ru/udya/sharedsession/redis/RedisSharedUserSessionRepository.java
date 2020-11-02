package ru.udya.sharedsession.redis;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.Access;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.group.ConstraintsContainer;
import com.haulmont.cuba.security.role.RoleDefinition;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.cache.SharedUserSessionCache;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionNotFoundException;
import ru.udya.sharedsession.exception.SharedSessionOptimisticLockException;
import ru.udya.sharedsession.exception.SharedSessionPersistingException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionBuildHelper;
import ru.udya.sharedsession.redis.codec.RedisUserSessionCodec;
import ru.udya.sharedsession.repository.SharedUserSession;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;
import ru.udya.sharedsession.service.SharedUserPermissionRuntime;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

@Component(SharedUserSessionRepository.NAME)
public class RedisSharedUserSessionRepository
        implements SharedUserSessionRepository {

    public static final String KEY_PREFIX = "shared:session";

    protected SharedUserPermissionBuildHelper permissionConverter;

    protected RedisClient redisClient;
    protected SharedUserSessionCache sessionCache;
    protected SharedUserPermissionRuntime sharedUserPermissionRuntime;

    protected RedisCodec<String, UserSession> objectRedisCodec;
    protected StatefulRedisConnection<String, UserSession> asyncReadConnection;

    public RedisSharedUserSessionRepository(RedisClient redisClient,
                                            SharedUserSessionCache sessionCache) {
        this.redisClient = redisClient;
        this.sessionCache = sessionCache;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.objectRedisCodec = RedisUserSessionCodec.INSTANCE;
        this.asyncReadConnection = redisClient.connect(objectRedisCodec);
    }

    @Override
    public UserSession createSession(UserSession src) {
        RedisSharedUserSession sharedUserSession
                = new RedisSharedUserSession(src);

        // save session in redis during creation
        sharedUserSession.save();

        return sharedUserSession;
    }

    @Override
    public void save(UserSession session) {
        if (session instanceof RedisSharedUserSession) {
            // everything is saved if it is redis session
            return;
        }

        createSession(session);
    }

    @Override
    public UserSession findById(UUID id) {
        // todo check that session is exist
        return new RedisSharedUserSession(id);
    }

    @Override
    public List<UserSession> findAllUserSessions() {
        // todo implement
        return Collections.emptyList();
    }

    @Override
    public void delete(UserSession id) {
        // todo implement
        throw new NotImplementedException("Will be implemented in a future");
    }

    @Override
    public void deleteById(UserSession session) {
        // todo implement
        throw new NotImplementedException("Will be implemented in a future");
    }

    @PreDestroy
    public void close() {
        asyncReadConnection.close();
    }

    protected RedisSharedUserSession findBySessionKeyNoCache(String sessionKey) {
        try {

            UserSession userSession = asyncReadConnection.async()
                    .get(sessionKey).get();

            return new RedisSharedUserSession(userSession, sessionKey);

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

    protected String createSessionKey(UUID id) {
        return KEY_PREFIX + ":" + id;
    }

    protected class RedisSharedUserSession extends UserSession
            implements SharedUserSession {

        private static final long serialVersionUID = 453371678445414846L;

        protected String sessionKey;

        // use delegate to apply side-effects in getter/setter UserSession
        protected UserSession delegate;

        public RedisSharedUserSession(UUID id) {
            this.id = id;
            this.sessionKey = createSessionKey(id);
        }

        public RedisSharedUserSession(UserSession userSession) {
            this(userSession, createSessionKey(userSession.getId()));
        }

        public RedisSharedUserSession(UserSession userSession, String sessionKey) {
            this.id = userSession.getId();
            this.delegate = userSession;
            this.sessionKey = sessionKey;
        }

        protected void save() {
            try {
                asyncReadConnection.async()
                        .set(sessionKey, delegate)
                        .get();

                sessionCache.saveInCache(sessionKey, this);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SharedSessionPersistingException("Thread is interrupted by external process during persisting user session", e);
            } catch (ExecutionException e) {
                throw new SharedSessionPersistingException("Exception during persisting user session", e);
            } catch (RedisCommandTimeoutException e) {
                throw new SharedSessionTimeoutException(e);
            } catch (RedisException e) {
                throw new SharedSessionException(e);
            }
        }

        protected UserSession safeUpdatingValue(Consumer<RedisSharedUserSession> updateFn) {

            try (StatefulRedisConnection<String, UserSession> writeConnection =
                         redisClient.connect(objectRedisCodec)) {

                RedisCommands<String, UserSession> sync = writeConnection.sync();

                sync.watch(sessionKey);

                UserSession sessionFromRedis = sync.get(sessionKey);
                if (sessionFromRedis == null) {
                    throw new SharedSessionNotFoundException(String.format("Session isn't found in Redis storage (Key: %s)", sessionKey));
                }

                RedisSharedUserSession updatedSharedUserSession =
                        new RedisSharedUserSession(sessionFromRedis, sessionKey);

                // apply setter
                updateFn.accept(updatedSharedUserSession);

                sync.multi();
                sync.set(sessionKey, sessionFromRedis);

                TransactionResult transactionResult = sync.exec();
                if (transactionResult.wasDiscarded()) {

                    sessionCache.removeFromCache(sessionKey);

                    throw new SharedSessionOptimisticLockException(
                            "Session changes can't be saved to Redis because someone changes session in Redis during transaction");
                }

                sessionCache.saveInCache(sessionKey, updatedSharedUserSession);

                return updatedSharedUserSession;
            } catch (RedisCommandTimeoutException e) {
                throw new SharedSessionTimeoutException(e);
            } catch (RedisException e) {
                throw new SharedSessionException(e);
            }
        }

        protected <T> T safeGettingValue(Function<RedisSharedUserSession, T> getter) {

            RedisSharedUserSession sharedUserSession = sessionCache
                    .getFromCacheBySessionKey(this.sessionKey,
                            RedisSharedUserSessionRepository.this::findBySessionKeyNoCache);

            return getter.apply(sharedUserSession);
        }

        // permission

        @Override
        public boolean isScreenPermitted(String windowAlias) {
            var permission = permissionConverter
                    .buildPermissionByWindowAlias(windowAlias);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUser(permission, Id.of(id, User.class));
        }

        @Override
        public boolean isEntityOpPermitted(MetaClass metaClass, EntityOp entityOp) {
            var permission = permissionConverter
                    .buildPermissionByEntity(metaClass, entityOp);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUser(permission, Id.of(id, User.class));
        }

        @Override
        public boolean isEntityAttrPermitted(MetaClass metaClass, String property,
                                             EntityAttrAccess access) {
            var permission = permissionConverter
                    .buildPermissionByEntityAttribute(metaClass, property, access);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUser(permission, Id.of(id, User.class));
        }

        @Override
        public boolean isSpecificPermitted(String name) {
            var permission = permissionConverter
                    .buildPermissionBySpecificPermission(name);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUser(permission, Id.of(id, User.class));
        }

        @Override
        public boolean isPermitted(PermissionType type, String target) {
            var permission = permissionConverter
                    .buildPermission(type, target);


            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUser(permission, Id.of(id, User.class));
        }

        @Override
        public boolean isPermitted(PermissionType type, String target, int value) {
            var permission = permissionConverter
                    .buildPermission(type, target, value);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUser(permission, Id.of(id, User.class));
        }

        @Override
        public Integer getPermissionValue(PermissionType type,
                                          String target) {
            return safeGettingValue(us -> us.delegate.getPermissionValue(type, target));
        }

        @Override
        public Map<String, Integer> getPermissionsByType(
                PermissionType type) {
            return safeGettingValue(us -> us.delegate.getPermissionsByType(type));
        }


        @Override
        public Access getPermissionUndefinedAccessPolicy() {
            return safeGettingValue(us -> us.delegate.getPermissionUndefinedAccessPolicy());
        }

        @Override
        public void setPermissionUndefinedAccessPolicy(
                Access permissionUndefinedAccessPolicy) {
            safeUpdatingValue(us -> us.delegate.setPermissionUndefinedAccessPolicy(permissionUndefinedAccessPolicy));
        }

        // boilerplate

        @Override
        public UUID getId() {
            return this.id;
        }

        @Override
        public User getUser() {
            return safeGettingValue(us -> us.delegate.getUser());
        }

        @Override
        public void setUser(User user) {
            safeUpdatingValue(us -> us.delegate.setUser(user));
        }

        @Override
        public User getSubstitutedUser() {
            return safeGettingValue(us -> us.delegate.getSubstitutedUser());
        }

        @Override
        public void setSubstitutedUser(User substitutedUser) {
            safeUpdatingValue(us -> us.delegate.setSubstitutedUser(substitutedUser));
        }

        @Override
        public User getCurrentOrSubstitutedUser() {
            return safeGettingValue(us -> us.delegate.getCurrentOrSubstitutedUser());
        }

        @Override
        public Collection<String> getRoles() {
            return safeGettingValue(us -> us.delegate.getRoles());
        }

        @Override
        public Locale getLocale() {
            return safeGettingValue(us -> us.delegate.getLocale());
        }

        @Override
        public void setLocale(Locale locale) {
            safeUpdatingValue(us -> us.delegate.setLocale(locale));
        }

        @Override
        @Nullable
        public TimeZone getTimeZone() {
            return safeGettingValue(us -> us.delegate.getTimeZone());
        }

        @Override
        public void setTimeZone(TimeZone timeZone) {
            safeUpdatingValue(us -> us.delegate.setTimeZone(timeZone));
        }

        @Override
        public String getAddress() {
            return safeGettingValue(us -> us.delegate.getAddress());
        }

        @Override
        public void setAddress(String address) {
            safeUpdatingValue(us -> us.delegate.setAddress(address));
        }

        @Override
        public String getClientInfo() {
            return safeGettingValue(us -> us.delegate.getClientInfo());
        }

        @Override
        public void setClientInfo(String clientInfo) {
            safeUpdatingValue(us -> us.delegate.setClientInfo(clientInfo));
        }

        @Override
        @Nullable
        public <T> T getAttribute(String name) {
            return safeGettingValue(us -> us.delegate.getAttribute(name));
        }

        @Override
        public void removeAttribute(String name) {
            safeUpdatingValue(us -> us.delegate.removeAttribute(name));
        }

        @Override
        public void setAttribute(String name, Serializable value) {
            safeUpdatingValue(us -> us.delegate.setAttribute(name, value));
        }

        @Override
        public Collection<String> getAttributeNames() {
            return safeGettingValue(us -> us.delegate.getAttributeNames());
        }

        @Override
        public boolean isSystem() {
            return safeGettingValue(us -> us.delegate.isSystem());
        }

        @Override
        public RoleDefinition getJoinedRole() {
            return safeGettingValue(us -> us.delegate.getJoinedRole());
        }

        @Override
        public void setJoinedRole(RoleDefinition joinedRole) {
            safeUpdatingValue(us -> us.delegate.setJoinedRole(joinedRole));
        }

        @Override
        public ConstraintsContainer getConstraints() {
            return safeGettingValue(us -> us.delegate.getConstraints());
        }

        @Override
        public void setConstraints(ConstraintsContainer constraints) {
            safeUpdatingValue(us -> us.delegate.setConstraints(constraints));
        }

        @Override
        public String toString() {
            return "RedisSharedUserSession{" +
                    "sessionKey='" + sessionKey + '\'' +
                    "} ";
        }
    }
}
