package ru.udya.sharedsession.redis;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.security.entity.Access;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.group.ConstraintsContainer;
import com.haulmont.cuba.security.role.RoleDefinition;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.permission.helper.CubaPermissionBuildHelper;
import ru.udya.sharedsession.permission.helper.CubaPermissionStringRepresentationHelper;
import ru.udya.sharedsession.permission.helper.CubaPermissionStringRepresentationHelper.CubaPermission;
import ru.udya.sharedsession.permission.helper.SharedUserPermissionBuildHelper;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.redis.permission.repository.RedisSharedUserSessionPermissionRepository;
import ru.udya.sharedsession.redis.permission.runtime.RedisSharedUserSessionPermissionRuntime;
import ru.udya.sharedsession.redis.repository.RedisSharedUserSessionRepository;
import ru.udya.sharedsession.redis.runtime.RedisSharedUserSessionRuntime;
import ru.udya.sharedsession.redis.tool.RedisSharedUserSessionIdTool;
import ru.udya.sharedsession.repository.SharedUserSessionRuntimeAdapter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

// todo rename to Runtime and create real Repository
@Component(SharedUserSessionRuntimeAdapter.NAME)
public class RedisSharedUserSessionRuntimeAdapter
        implements SharedUserSessionRuntimeAdapter {

    protected SharedUserPermissionBuildHelper sharedPermissionBuildHelper;
    protected CubaPermissionStringRepresentationHelper cubaPermissionStringRepresentationHelper;
    protected CubaPermissionBuildHelper cubaPermissionBuildHelper;

    protected RedisSharedUserSessionIdTool redisSharedUserSessionIdTool;

    protected RedisSharedUserSessionRuntime sharedUserSessionRuntime;
    protected RedisSharedUserSessionPermissionRuntime sharedUserPermissionRuntime;

    protected RedisSharedUserSessionRepository sharedUserSessionRepository;
    protected RedisSharedUserSessionPermissionRepository sharedUserPermissionRepository;

    public RedisSharedUserSessionRuntimeAdapter(
            SharedUserPermissionBuildHelper sharedPermissionBuildHelper,
            CubaPermissionStringRepresentationHelper cubaPermissionStringRepresentationHelper,
            CubaPermissionBuildHelper cubaPermissionBuildHelper,
            RedisSharedUserSessionIdTool redisSharedUserSessionIdTool,
            RedisSharedUserSessionRuntime sharedUserSessionRuntime,
            RedisSharedUserSessionPermissionRuntime sharedUserPermissionRuntime,
            RedisSharedUserSessionRepository sharedUserSessionRepository,
            RedisSharedUserSessionPermissionRepository sharedUserPermissionRepository) {
        this.sharedPermissionBuildHelper = sharedPermissionBuildHelper;
        this.cubaPermissionStringRepresentationHelper = cubaPermissionStringRepresentationHelper;
        this.cubaPermissionBuildHelper = cubaPermissionBuildHelper;
        this.redisSharedUserSessionIdTool = redisSharedUserSessionIdTool;
        this.sharedUserSessionRuntime = sharedUserSessionRuntime;
        this.sharedUserPermissionRuntime = sharedUserPermissionRuntime;
        this.sharedUserSessionRepository = sharedUserSessionRepository;
        this.sharedUserPermissionRepository = sharedUserPermissionRepository;
    }

    @Override
    public UserSession createSession(UserSession cubaUserSession) {

        var sharedUserSessionId = sharedUserSessionRuntime
                .createByCubaUserSession(cubaUserSession);

        return new RedisSharedUserSessionAdapter(sharedUserSessionId);
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
    public UserSession findByCubaUserSessionId(UUID id) {
        var sharedId = sharedUserSessionRepository
                .findIdByCubaUserSessionId(id);

        return new RedisSharedUserSessionAdapter(sharedId);
    }

    @Override
    public UserSession findBySharedId(Serializable id) {

        return new RedisSharedUserSessionAdapter((String) id);
    }

    protected class RedisSharedUserSessionAdapter extends UserSession
            implements RedisSharedUserSessionId {

        private static final long serialVersionUID = 453371678445414846L;

        // there are final because they should be filled in any cases of creation
        protected final UUID cubaSessionId;

        protected final RedisSharedUserSessionId sharedUserSessionId;

        public RedisSharedUserSessionAdapter(
                RedisSharedUserSessionId sharedUserSessionId) {
            this.sharedUserSessionId = sharedUserSessionId;
            this.cubaSessionId = redisSharedUserSessionIdTool
                    .extractCubaUserSessionIdFromSharedUserSessionId(sharedUserSessionId);
        }

        public RedisSharedUserSessionAdapter(String sharedUserSessionIdStringRepresentation) {
            var sharedUserSessionId = RedisSharedUserSessionId.of(sharedUserSessionIdStringRepresentation);

            this.sharedUserSessionId = sharedUserSessionId;
            this.cubaSessionId = redisSharedUserSessionIdTool
                    .extractCubaUserSessionIdFromSharedUserSessionId(sharedUserSessionId);
        }

        @SuppressWarnings("UnusedReturnValue")
        protected RedisSharedUserSession safeUpdatingValue(Consumer<RedisSharedUserSession> updateFn) {

            return sharedUserSessionRepository.updateByFn(sharedUserSessionId, updateFn);
        }

        protected <T> T safeGettingValue(Function<RedisSharedUserSession, T> getter) {

            var sharedUserSession = sharedUserSessionRepository.findById(sharedUserSessionId);

            return getter.apply(sharedUserSession);
        }

        // id

        @Override
        public UUID getId() {
            return cubaSessionId;
        }

        @Override
        public String getSharedId() {
            return sharedUserSessionId.getSharedId();
        }


        // permission

        @Override
        public boolean isScreenPermitted(String windowAlias) {
            // https://www.cuba-platform.ru/discuss/t/windowalias-raven-null-pri-vyzove-isscreenpermitted-string-windowalias-clientusersession/5252/2
            if (windowAlias == null) {
                return true; // check on simple CUBA App
            }

            var permission = sharedPermissionBuildHelper
                    .buildPermissionByWindowAlias(windowAlias);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(sharedUserSessionId, permission);
        }

        @Override
        public boolean isEntityOpPermitted(MetaClass metaClass, EntityOp entityOp) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByEntity(metaClass, entityOp);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(sharedUserSessionId, permission);
        }

        @Override
        public boolean isEntityAttrPermitted(MetaClass metaClass, String property,
                                             EntityAttrAccess access) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByEntityAttribute(metaClass, property, access);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(sharedUserSessionId, permission);
        }

        @Override
        public boolean isSpecificPermitted(String name) {
            // https://www.cuba-platform.ru/discuss/t/windowalias-raven-null-pri-vyzove-isscreenpermitted-string-windowalias-clientusersession/5252/2
            if (name == null) {
                return true; // check on simple CUBA App
            }

            var permission = sharedPermissionBuildHelper
                    .buildPermissionBySpecificPermission(name);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(sharedUserSessionId, permission);
        }

        @Override
        public boolean isPermitted(PermissionType type, String target) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByCubaTarget(type, target);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(sharedUserSessionId, permission);
        }

        @Override
        public boolean isPermitted(PermissionType type, String target, int value) {
            var permission = sharedPermissionBuildHelper
                    .buildPermissionByCubaTarget(type, target, value);

            return sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(sharedUserSessionId, permission);
        }

        @Override
        public Integer getPermissionValue(PermissionType type, String target) {
            var sharedUserPermission = sharedPermissionBuildHelper
                    .buildPermissionByCubaTarget(type, target);

            var grantedToUserSession = sharedUserPermissionRuntime
                    .isPermissionGrantedToUserSession(sharedUserSessionId, sharedUserPermission);

            return grantedToUserSession ? 1 : 0;
        }

        @Override
        public Map<String, Integer> getPermissionsByType(PermissionType type) {

            Stream<CubaPermission> cubaPermissions = Stream.empty();

            switch (type) {
                case SCREEN:
                    var sharedUserScreenPermissions = sharedUserPermissionRepository
                            .findAllScreenPermissionsByUserSession(sharedUserSessionId);

                    cubaPermissions = sharedUserScreenPermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserScreenPermissionToCubaPermission);

                    break;
                case ENTITY_OP:
                    var sharedUserEntityPermissions = sharedUserPermissionRepository
                            .findAllEntityPermissionsByUserSession(sharedUserSessionId);

                    cubaPermissions = sharedUserEntityPermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserEntityPermissionToCubaPermission);
                    break;
                case ENTITY_ATTR:
                    var sharedUserEntityAttributePermissions = sharedUserPermissionRepository
                            .findAllEntityAttributePermissionsByUserSession(sharedUserSessionId);

                    cubaPermissions = sharedUserEntityAttributePermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserEntityAttributePermissionToCubaPermission);
                    break;
                case SPECIFIC:
                    var sharedUserSpecificPermissions = sharedUserPermissionRepository
                            .findAllSpecificPermissionsByUserSession(sharedUserSessionId);

                    cubaPermissions = sharedUserSpecificPermissions
                            .stream()
                            .map(cubaPermissionStringRepresentationHelper
                                         ::convertSharedUserSpecificPermissionToCubaPermission);
                    break;
                case UI:
                    var sharedUserScreenElementPermissions = sharedUserPermissionRepository
                            .findAllScreenElementPermissionsByUserSession(sharedUserSessionId);

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
                    .findAllByUserSession(sharedUserSessionId);


            return cubaPermissionBuildHelper
                    .buildRoleDefinitionBySharedUserPermissions(sharedUserPermissions);
        }

        @Override
        public void setJoinedRole(RoleDefinition joinedRole) {
            var sharedUserPermissions = sharedPermissionBuildHelper
                    .buildPermissionsByCubaRoleDefinition(joinedRole);

            sharedUserPermissionRuntime
                    .grantPermissionsToUserSession(sharedUserSessionId, sharedUserPermissions);
        }

        // boilerplate

        @Override
        public User getUser() {
            return safeGettingValue(us -> us.getCubaUserSession().getUser());
        }

        @Override
        public void setUser(User user) {
            safeUpdatingValue(us -> us.getCubaUserSession().setUser(user));
        }

        @Override
        public User getSubstitutedUser() {
            return safeGettingValue(us -> us.getCubaUserSession().getSubstitutedUser());
        }

        @Override
        public void setSubstitutedUser(User substitutedUser) {
            safeUpdatingValue(us -> us.getCubaUserSession().setSubstitutedUser(substitutedUser));
        }

        @Override
        public User getCurrentOrSubstitutedUser() {
            return safeGettingValue(us -> us.getCubaUserSession().getCurrentOrSubstitutedUser());
        }

        @Override
        public Collection<String> getRoles() {
            return safeGettingValue(us -> us.getCubaUserSession().getRoles());
        }

        @Override
        public Locale getLocale() {
            return safeGettingValue(us -> us.getCubaUserSession().getLocale());
        }

        @Override
        public void setLocale(Locale locale) {
            safeUpdatingValue(us -> us.getCubaUserSession().setLocale(locale));
        }

        @Override
        @Nullable
        public TimeZone getTimeZone() {
            return safeGettingValue(us -> us.getCubaUserSession().getTimeZone());
        }

        @Override
        public void setTimeZone(TimeZone timeZone) {
            safeUpdatingValue(us -> us.getCubaUserSession().setTimeZone(timeZone));
        }

        @Override
        public String getAddress() {
            return safeGettingValue(us -> us.getCubaUserSession().getAddress());
        }

        @Override
        public void setAddress(String address) {
            safeUpdatingValue(us -> us.getCubaUserSession().setAddress(address));
        }

        @Override
        public String getClientInfo() {
            return safeGettingValue(us -> us.getCubaUserSession().getClientInfo());
        }

        @Override
        public void setClientInfo(String clientInfo) {
            safeUpdatingValue(us -> us.getCubaUserSession().setClientInfo(clientInfo));
        }

        @Override
        @Nullable
        public <T> T getAttribute(String name) {
            return safeGettingValue(us -> us.getCubaUserSession().getAttribute(name));
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
            return safeGettingValue(us -> us.getCubaUserSession().getAttributeNames());
        }

        @Override
        public boolean isSystem() {
            return safeGettingValue(us -> us.getCubaUserSession().isSystem());
        }

        @Override
        public ConstraintsContainer getConstraints() {
            return safeGettingValue(us -> us.getCubaUserSession().getConstraints());
        }

        @Override
        public void setConstraints(ConstraintsContainer constraints) {
            safeUpdatingValue(us -> us.getCubaUserSession().setConstraints(constraints));
        }

        @Override
        public String toString() {
            return "RedisSharedUserSessionAdapter{" +
                   "sessionKey='" + sharedUserSessionId + '\'' +
                   "} ";
        }
    }
}
