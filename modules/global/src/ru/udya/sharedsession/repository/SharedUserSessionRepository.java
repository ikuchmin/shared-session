package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.domain.SharedUserSessionId;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface SharedUserSessionRepository {

    String NAME = "ss_SharedUserSessionRepository";

    SharedUserSession findById(Serializable sharedId);

    List<SharedUserSession> findAllByUser(Id<User, UUID> userId);

    List<SharedUserSessionId> findAllKeysByUser(Id<User, UUID> userId);

    void save(SharedUserSession sharedUserSession);
}
