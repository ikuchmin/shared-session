package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.role.RoleDefinition;
import ru.udya.sharedsession.domain.SharedUserSession;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Inspired by {@link org.springframework.session.SessionRepository}
 */
public interface SharedUserSessionRepository {

    String NAME = "ss_SharedUserSessionRepository";

    default UserSession createSession(UUID sessionId, User user,
                                      Collection<RoleDefinition> roles,
                                      Locale locale, boolean system) {
        return createSession(new UserSession(sessionId, user, roles, locale, system));
    }

    default UserSession createSession(UserSession src, User user,
                                      Collection<RoleDefinition> roles,
                                      Locale locale) {
        return createSession(new UserSession(src, user, roles, locale));
    }

    UserSession createSession(UserSession src);

    void save(UserSession session);

    UserSession findById(Serializable id);

    List<UserSession> findAll();

    List<SharedUserSession> findAllByUser(Id<User, UUID> userId);

    List<SharedUserSession> findAllByUsers(Ids<User, UUID> userId);

    void delete(UserSession session);

    void deleteById(UserSession session);
}
