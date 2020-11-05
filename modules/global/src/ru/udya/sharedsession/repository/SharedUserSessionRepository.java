package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.domain.SharedUserSessionId;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface SharedUserSessionRepository<ID extends Serializable> {

    String NAME = "ss_SharedUserSessionRepository";

    SharedUserSession<ID> findById(ID sharedId);

    List<? extends SharedUserSession<ID>> findAllByUser(Id<User, UUID> userId);

    List<? extends SharedUserSessionId<ID>> findAllKeysByUser(Id<User, UUID> userId);

    void save(SharedUserSession<ID> sharedUserSession);
}
