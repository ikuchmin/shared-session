package ru.udya.sharedsession.redis;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.core.global.UuidProvider;
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
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.cache.SharedUserSessionCache;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.exception.SharedSessionException;
import ru.udya.sharedsession.exception.SharedSessionOptimisticLockException;
import ru.udya.sharedsession.exception.SharedSessionReadingException;
import ru.udya.sharedsession.exception.SharedSessionTimeoutException;
import ru.udya.sharedsession.permission.helper.CubaPermissionBuildHelper;
import ru.udya.sharedsession.permission.helper.CubaPermissionStringRepresentationHelper;
import ru.udya.sharedsession.permission.helper.CubaPermissionStringRepresentationHelper.CubaPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionBuildHelper;
import ru.udya.sharedsession.redis.codec.RedisUserSessionCodec;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserPermissionRepository;
import ru.udya.sharedsession.redis.permission.runtime.RedisSharedUserPermissionRuntime;
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;
import ru.udya.sharedsession.repository.SharedUserSessionRuntimeAdapter;

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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

// todo rename to Runtime and create real Repository
@Component(SharedUserSessionRuntimeAdapter.NAME)
public class RedisSharedUserSessionRuntime
        implements SharedUserSessionRuntimeAdapter {

    public static final String KEY_PREFIX = "shared:session";

    // shared:session:userId:sessionId
    public static final String KEY_PATTERN = KEY_PREFIX + ":" + "%s:%s";

    protected RedisClient redisClient;
    protected SharedUserSessionCache sessionCache;

    protected SharedUserPermissionBuildHelper sharedPermissionBuildHelper;
    protected CubaPermissionStringRepresentationHelper cubaPermissionStringRepresentationHelper;
    protected CubaPermissionBuildHelper cubaPermissionBuildHelper;

    protected RedisSharedUserSessionRepository sharedUserSessionRepository;
    protected RedisSharedUserPermissionRuntime sharedUserPermissionRuntime;
    protected RedisSharedUserPermissionRepository sharedUserPermissionRepository;

    protected RedisCodec<String, UserSession> objectRedisCodec;
    protected StatefulRedisConnection<String, UserSession> asyncReadConnection;

    public RedisSharedUserSessionRuntime(RedisClient redisClient,
                                         SharedUserSessionCache sessionCache,
                                         SharedUserPermissionBuildHelper sharedPermissionBuildHelper,
                                         CubaPermissionStringRepresentationHelper cubaPermissionStringRepresentationHelper,
                                         CubaPermissionBuildHelper cubaPermissionBuildHelper,
                                         RedisSharedUserPermissionRuntime sharedUserPermissionRuntime,
                                         RedisSharedUserPermissionRepository sharedUserPermissionRepository) {
        this.redisClient = redisClient;
        this.sessionCache = sessionCache;
        this.sharedPermissionBuildHelper = sharedPermissionBuildHelper;
        this.cubaPermissionStringRepresentationHelper = cubaPermissionStringRepresentationHelper;
        this.cubaPermissionBuildHelper = cubaPermissionBuildHelper;
        this.sharedUserPermissionRuntime = sharedUserPermissionRuntime;
        this.sharedUserPermissionRepository = sharedUserPermissionRepository;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        this.objectRedisCodec = RedisUserSessionCodec.INSTANCE;
        this.asyncReadConnection = redisClient.connect(objectRedisCodec);
    }

    @Override
    public UserSession createSession(UserSession src) {
        RedisSharedUserSessionAdapter sharedUserSession
                = new RedisSharedUserSessionAdapter(src);

        // save session in redis during creation
        sharedUserSession.save();

        return sharedUserSession;
    }

    @Override
    public void save(UserSession session) {
        if (session instanceof RedisSharedUserSessionAdapter) {
            // everything is saved if it is redis session
            return;
        }

        createSession(session);
    }

    @Override
    public UserSession findById(Serializable id) {
        // todo check that session is exist
        return new RedisSharedUserSessionAdapter((String) id);
    }

    @Override
    public List<UserSession> findAll() {
        // todo implement
        return Collections.emptyList();
    }

    @Override
    public List<SharedUserSession> findAllByUser(Id<User, UUID> userId) {
        throw new UnsupportedOperationException("Will be implemented later");
    }

    @Override
    public List<SharedUserSession> findAllByUsers(Ids<User, UUID> userId) {
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

    protected RedisSharedUserSessionAdapter findBySessionKeyNoCache(String sessionKey) {
        try {

            UserSession userSession = asyncReadConnection.async()
                    .get(sessionKey).get();

            return new RedisSharedUserSessionAdapter(userSession, sessionKey);

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

    // don't use Id<> because it is internal API
    protected String createSharedUserSessionKey(UUID userId, UUID sessionId) {
        return String.format(KEY_PATTERN, userId, sessionId);
    }

    protected String createSharedUserSessionKey(UserSession userSession) {
        return createSharedUserSessionKey(userSession.getUser().getId(), userSession.getId());
    }

    protected UUID extractUserSessionIdFromSharedUserSessionKey(String sharedUserSessionKey) {
        var sessionKeyParts = sharedUserSessionKey.split(":");
        return UuidProvider.fromString(sessionKeyParts[3]);
    }

    protected class RedisSharedUserSessionAdapter extends UserSession
            implements SharedUserSession {

        private static final long serialVersionUID = 453371678445414846L;

        protected RedisSharedUserSessionId sharedUserSessionId;
        protected String sharedId;

        // use delegate to apply side-effects in getter/setter UserSession
        protected UserSession delegate;

        protected RedisSharedUserSession redisSharedUserSession;

        public RedisSharedUserSessionAdapter(String sharedId) {
            this.sharedId = sharedId;
            this.id = extractUserSessionIdFromSharedUserSessionKey(sharedId);
        }

        public RedisSharedUserSessionAdapter(Id<User, UUID> userId, UUID sessionId) {
            this.id = sessionId;
            this.sharedId = createSharedUserSessionKey(userId.getValue(), sessionId);
        }

        public RedisSharedUserSessionAdapter(UserSession userSession) {
            this(userSession, createSharedUserSessionKey(userSession));
        }

        public RedisSharedUserSessionAdapter(UserSession userSession, String sharedId) {
            this.id = userSession.getId();
            this.delegate = userSession;
            this.sharedId = sharedId;
        }

        protected void save() {
            var roleDefinition = delegate.getJoinedRole();

            // clean role definition because it has additional support below
            //noinspection ConstantConditions
            delegate.setJoinedRole(null);
            sharedUserSessionRepository.save(redisSharedUserSession);

            var sharedUserPermissions = sharedPermissionBuildHelper
                    .buildPermissionsByCubaRoleDefinition(roleDefinition);

            sharedUserPermissionRuntime.grantPermissionsToUserSession(
                    redisSharedUserSession, sharedUserPermissions);

            sessionCache.saveInCache(redisSharedUserSession);

        }

        @SuppressWarnings("UnusedReturnValue")
        protected UserSession safeUpdatingValue(Consumer<RedisSharedUserSession> updateFn) {

            try {
                var updatedSharedUserSession = sharedUserSessionRepository
                        .updateByFn(sharedUserSessionId, updateFn);

                sessionCache.saveInCache(updatedSharedUserSession);

            } catch (SharedSessionOptimisticLockException optimisticLockException) {

                sessionCache.removeFromCache(sharedUserSessionId.getSharedId());

                throw optimisticLockException;
            }
        }

        protected <T> T safeGettingValue(Function<RedisSharedUserSessionAdapter, T> getter) {

            RedisSharedUserSessionAdapter sharedUserSession = sessionCache
                    .getFromCacheBySessionKey(this.sharedId,
                            RedisSharedUserSessionRuntime.this::findBySessionKeyNoCache);

            return getter.apply(sharedUserSession);
        }

        // permission

        @Override
        public boolean isScreenPermitted(String windowAlias) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByWindowAlias(windowAlias);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(this, permission);
        }

        @Override
        public boolean isEntityOpPermitted(MetaClass metaClass, EntityOp entityOp) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByEntity(metaClass, entityOp);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(this, permission);
        }

        @Override
        public boolean isEntityAttrPermitted(MetaClass metaClass, String property,
                                             EntityAttrAccess access) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByEntityAttribute(metaClass, property, access);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(this, permission);
        }

        @Override
        public boolean isSpecificPermitted(String name) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionBySpecificPermission(name);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(this, permission);
        }

        @Override
        public boolean isPermitted(PermissionType type, String target) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByCubaTarget(type, target);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(this, permission);
        }

        @Override
        public boolean isPermitted(PermissionType type, String target, int value) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByCubaTarget(type, target, value);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(this, permission);
        }

        @Override
        public Integer getPermissionValue(PermissionType type, String target) {
            var sharedUserPermission = sharedPermissionBuildHelper
                    .buildPermissionByCubaTarget(type, target);

            var grantedToUserSession = sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(this, sharedUserPermission);

            return grantedToUserSession ? 1 : 0;
        }

        @Override
        public Map<String, Integer> getPermissionsByType(PermissionType type) {

            Stream<CubaPermission> cubaPermissions = Stream.empty();

            switch (type) {
                case SCREEN:
                    var sharedUserScreenPermissions = sharedUserPermissionRepository
                            .findAllScreenPermissionsByUserSession(this);

                    cubaPermissions = sharedUserScreenPermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserScreenPermissionToCubaPermission);

                    break;
                case ENTITY_OP:
                    var sharedUserEntityPermissions = sharedUserPermissionRepository
                            .findAllEntityPermissionsByUserSession(this);

                    cubaPermissions = sharedUserEntityPermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserEntityPermissionToCubaPermission);
                    break;
                case ENTITY_ATTR:
                    var sharedUserEntityAttributePermissions = sharedUserPermissionRepository
                            .findAllEntityAttributePermissionsByUserSession(this);

                    cubaPermissions = sharedUserEntityAttributePermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserEntityAttributePermissionToCubaPermission);
                    break;
                case SPECIFIC:
                    var sharedUserSpecificPermissions = sharedUserPermissionRepository
                            .findAllSpecificPermissionsByUserSession(this);

                    cubaPermissions = sharedUserSpecificPermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserSpecificPermissionToCubaPermission);
                    break;
                case UI:
                    var sharedUserScreenElementPermissions = sharedUserPermissionRepository
                            .findAllScreenElementPermissionsByUserSession(this);

                    cubaPermissions = sharedUserScreenElementPermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserScreenElementPermissionToCubaPermission);

                    break;
            }

            return cubaPermissions.collect(toMap(CubaPermission::getTarget, CubaPermission::getValue));
        }


        @Override
        public Access getPermissionUndefinedAccessPolicy() {
            return Access.DENY;
        }

        @Override
        public void setPermissionUndefinedAccessPolicy(Access permissionUndefinedAccessPolicy) {
            // do noting, user can't change default access policy
        }

        @Override
        public RoleDefinition getJoinedRole() {
            var sharedUserPermissions = sharedUserPermissionRepository
                    .findAllByUserSession(this);


            return cubaPermissionBuildHelper
                    .buildRoleDefinitionBySharedUserPermissions(sharedUserPermissions);
        }

        @Override
        public void setJoinedRole(RoleDefinition joinedRole) {
            var sharedUserPermissions = sharedPermissionBuildHelper
                    .buildPermissionsByCubaRoleDefinition(joinedRole);

            sharedUserPermissionRuntime
                    .grantPermissionsToUserSession(this, sharedUserPermissions);
        }

        // boilerplate

        @Override
        public String getSharedId() {
            return this.sharedId;
        }

        @Override
        public User getUser() {
            return safeGettingValue(us -> us.delegate.getUser());
        }

        @Override
        public void setUser(User user) {
            safeUpdatingValue(us -> us.getCubaUserSession().setUser(user));
        }

        @Override
        public User getSubstitutedUser() {
            return safeGettingValue(us -> us.delegate.getSubstitutedUser());
        }

        @Override
        public void setSubstitutedUser(User substitutedUser) {
            safeUpdatingValue(us -> us.getCubaUserSession().setSubstitutedUser(substitutedUser));
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
            safeUpdatingValue(us -> us.getCubaUserSession().setLocale(locale));
        }

        @Override
        @Nullable
        public TimeZone getTimeZone() {
            return safeGettingValue(us -> us.delegate.getTimeZone());
        }

        @Override
        public void setTimeZone(TimeZone timeZone) {
            safeUpdatingValue(us -> us.getCubaUserSession().setTimeZone(timeZone));
        }

        @Override
        public String getAddress() {
            return safeGettingValue(us -> us.delegate.getAddress());
        }

        @Override
        public void setAddress(String address) {
            safeUpdatingValue(us -> us.getCubaUserSession().setAddress(address));
        }

        @Override
        public String getClientInfo() {
            return safeGettingValue(us -> us.delegate.getClientInfo());
        }

        @Override
        public void setClientInfo(String clientInfo) {
            safeUpdatingValue(us -> us.getCubaUserSession().setClientInfo(clientInfo));
        }

        @Override
        @Nullable
        public <T> T getAttribute(String name) {
            return safeGettingValue(us -> us.delegate.getAttribute(name));
        }

        @Override
        public void removeAttribute(String name) {
            safeUpdatingValue(us -> us.getCubaUserSession().removeAttribute(name));
        }

        @Override
        public void setAttribute(String name, Serializable value) {
            safeUpdatingValue(us -> us.getCubaUserSession().setAttribute(name, value));
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
        public ConstraintsContainer getConstraints() {
            return safeGettingValue(us -> us.delegate.getConstraints());
        }

        @Override
        public void setConstraints(ConstraintsContainer constraints) {
            safeUpdatingValue(us -> us.getCubaUserSession().setConstraints(constraints));
        }

        @Override
        public String toString() {
            return "RedisSharedUserSessionAdapter{" +
                   "sessionKey='" + sharedId + '\'' +
                   "} ";
        }
    }
}
