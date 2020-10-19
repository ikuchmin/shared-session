package ru.udya.sharedsession.redis;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.security.entity.Access;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.group.ConstraintsContainer;
import com.haulmont.cuba.security.role.RoleDefinition;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionNotFoundException;
import ru.udya.sharedsession.exception.SharedSessionOptimisticLockException;
import ru.udya.sharedsession.exception.SharedSessionPersistingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.repository.SharedUserSession;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.udya.sharedsession.redis.RedisSharedUserSessionRepository.KEY_PREFIX;

public class RedisSharedUserSession extends UserSession
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
        this(userSession, null);
        sessionKey = createSessionKey(userSession.getId());
    }

    public RedisSharedUserSession(UserSession userSession, String sessionKey) {
        this.id = userSession.getId();
        this.delegate = userSession;
        this.sessionKey = sessionKey;
    }

    public String createSessionKey(UUID id) {
        return KEY_PREFIX + ":" + id;
    }

    protected void save() {
        try {
            SharedUserSessionRepository sharedUserSessionRepository = AppBeans.get(SharedUserSessionRepository.class);
            sharedUserSessionRepository.getAsyncReadConnection().async()
                    .set(sessionKey, delegate)
                    .get();

            sharedUserSessionRepository.saveInCache(sessionKey, this);

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
        SharedUserSessionRepository sharedUserSessionRepository = AppBeans.get(SharedUserSessionRepository.class);
        try (StatefulRedisConnection<String, UserSession> writeConnection =
                     sharedUserSessionRepository.getRedisClient()
                             .connect(sharedUserSessionRepository.getObjectRedisCodec())) {

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

                sharedUserSessionRepository.getSessionCache().removeFromCache(sessionKey);

                throw new SharedSessionOptimisticLockException(
                        "Session changes can't be saved to Redis because someone changes session in Redis during transaction");
            }

            sharedUserSessionRepository.saveInCache(sessionKey, updatedSharedUserSession);

            return updatedSharedUserSession;
        } catch (RedisCommandTimeoutException e) {
            throw new SharedSessionTimeoutException(e);
        } catch (RedisException e) {
            throw new SharedSessionException(e);
        }
    }

    protected <T> T safeGettingValue(Function<RedisSharedUserSession, T> getter) {
        SharedUserSessionRepository sharedUserSessionRepository = AppBeans.get(SharedUserSessionRepository.class);

        RedisSharedUserSession sharedUserSession = sharedUserSessionRepository.getSessionCache()
                .getFromCacheBySessionKey(this.sessionKey,
                        sharedUserSessionRepository::findBySessionKeyNoCache);

        return getter.apply(sharedUserSession);
    }


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
    public boolean isScreenPermitted(String windowAlias) {
        return safeGettingValue(us -> us.delegate.isScreenPermitted(windowAlias));
    }

    @Override
    public boolean isEntityOpPermitted(MetaClass metaClass,
                                       EntityOp entityOp) {
        return safeGettingValue(us -> us.delegate.isEntityOpPermitted(metaClass, entityOp));
    }

    @Override
    public boolean isEntityAttrPermitted(MetaClass metaClass,
                                         String property,
                                         EntityAttrAccess access) {
        return safeGettingValue(us -> us.delegate.isEntityAttrPermitted(metaClass, property, access));
    }

    @Override
    public boolean isSpecificPermitted(String name) {
        return safeGettingValue(us -> us.delegate.isSpecificPermitted(name));
    }

    @Override
    public boolean isPermitted(PermissionType type, String target) {
        return safeGettingValue(us -> us.delegate.isPermitted(type, target));
    }

    @Override
    public boolean isPermitted(PermissionType type, String target, int value) {
        return safeGettingValue(us -> us.delegate.isPermitted(type, target, value));
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
    public Access getPermissionUndefinedAccessPolicy() {
        return safeGettingValue(us -> us.delegate.getPermissionUndefinedAccessPolicy());
    }

    @Override
    public void setPermissionUndefinedAccessPolicy(
            Access permissionUndefinedAccessPolicy) {
        safeUpdatingValue(us -> us.delegate.setPermissionUndefinedAccessPolicy(permissionUndefinedAccessPolicy));
    }

    @Override
    public String toString() {
        return "RedisSharedUserSession{" +
                "sessionKey='" + sessionKey + '\'' +
                "} ";
    }
}
