package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.domain.SharedUserSessionId;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface SharedUserSessionRepository<S extends SharedUserSession<ID>,
        SID extends SharedUserSessionId<ID>, ID extends Serializable> {

    String NAME = "ss_SharedUserSessionRepository";

    S findById(SID sharedId);

    List<S> findAllByUser(Id<User, UUID> userId);

    List<SID> findAllKeysByUser(Id<User, UUID> userId);

    List<SID> findAllKeysByUsers(Ids<User, UUID> userId);

    S createByCubaUserSession(UserSession cubaUserSession);

    void save(S sharedUserSession);
}
