package ru.udya.sharedsession.sys;

import com.google.common.base.Strings;
import com.haulmont.cuba.security.entity.SecurityScope;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.group.AccessGroupDefinition;
import com.haulmont.cuba.security.role.RoleDefinition;
import com.haulmont.cuba.security.sys.UserSessionManager;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.repository.SharedUserSessionRuntimeAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component(SharedUserSessionManager.NAME)
public class SharedUserSessionManager extends UserSessionManager {

    public static final String NAME = "ss_SharedUserSessionManager";

    protected SharedUserSessionRuntimeAdapter sharedUserSessionRepository;

    public SharedUserSessionManager(SharedUserSessionRuntimeAdapter sharedUserSessionRepository) {
        this.sharedUserSessionRepository = sharedUserSessionRepository;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public UserSession createSession(UUID sessionId, User user, Locale locale, boolean system, String securityScope) {
        // cuba begin
        List<RoleDefinition> roles = new ArrayList<>();

        for (RoleDefinition role : rolesHelper.getRoleDefinitionsForUser(user, false)) {
            if (role != null) {
                String expectedScope = securityScope == null ? SecurityScope.DEFAULT_SCOPE_NAME : securityScope;
                String actualScope = role.getSecurityScope() == null ? SecurityScope.DEFAULT_SCOPE_NAME : role.getSecurityScope();
                if (Objects.equals(expectedScope, actualScope)) {
                    roles.add(role);
                }
            }
        }
        // cuba end

        UserSession session = sharedUserSessionRepository
                .createSession(sessionId, user, roles, locale, system);

        // cuba begin
        compilePermissions(session, roles);

        if (user.getGroup() == null && Strings.isNullOrEmpty(user.getGroupNames())) {
            throw new IllegalStateException("User is not in a Group");
        }
        AccessGroupDefinition groupDefinition = compileGroupDefinition(user.getGroup(), user.getGroupNames());
        compileConstraints(session, groupDefinition);
        compileSessionAttributes(session, groupDefinition);
        session.setPermissionUndefinedAccessPolicy(rolesHelper.getPermissionUndefinedAccessPolicy());

        return session;
        // cuba end
    }

    @Override
    public UserSession createSession(UserSession src, User user) {
        // cuba begin
        List<RoleDefinition> roles = new ArrayList<>();
        for (RoleDefinition role : rolesHelper.getRoleDefinitionsForUser(user, false)) {
            if (role != null) {
                roles.add(role);
            }
        }
        // cuba end

        UserSession session = sharedUserSessionRepository
                .createSession(src, user, roles, src.getLocale());

        // cuba begin
        compilePermissions(session, roles);
        if (user.getGroup() == null && Strings.isNullOrEmpty(user.getGroupNames())) {
            throw new IllegalStateException("User is not in a Group");
        }

        AccessGroupDefinition groupDefinition = compileGroupDefinition(user.getGroup(), user.getGroupNames());
        compileConstraints(session, groupDefinition);
        compileSessionAttributes(session, groupDefinition);
        session.setPermissionUndefinedAccessPolicy(rolesHelper.getPermissionUndefinedAccessPolicy());

        return session;
        // cuba end
    }

}
