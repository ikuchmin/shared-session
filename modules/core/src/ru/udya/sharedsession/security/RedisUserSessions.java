package ru.udya.sharedsession.security;

import com.haulmont.cuba.security.app.UserSessionsAPI;
import com.haulmont.cuba.security.entity.UserSessionEntity;
import com.haulmont.cuba.security.global.NoUserSessionException;
import com.haulmont.cuba.security.global.UserSession;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class RedisUserSessions implements UserSessionsAPI {

    protected volatile int expirationTimeout = 1800;

    protected volatile int sendTimeout = 10;

    protected SharedUserSessionRepository sharedUserSessionRepository;

    public RedisUserSessions(SharedUserSessionRepository sharedUserSessionRepository) {
        this.sharedUserSessionRepository = sharedUserSessionRepository;
    }

    @Nullable
    @Override
    public UserSession get(UUID id) {
        return sharedUserSessionRepository.findById(id);
    }

    @Override
    public UserSession getNN(UUID id) {
        UserSession userSession = sharedUserSessionRepository.findById(id);

        if (userSession == null)
            throw new NoUserSessionException(id);

        return userSession;
    }

    @Nullable
    @Override
    public UserSession getAndRefresh(UUID id) {
        return sharedUserSessionRepository.findById(id);
    }

    @Override
    public UserSession getAndRefreshNN(UUID id) {
        UserSession userSession = sharedUserSessionRepository.findById(id);

        if (userSession == null)
            throw new NoUserSessionException(id);

        return userSession;
    }

    @Nullable
    @Override
    public UserSession getAndRefresh(UUID id, boolean propagate) {
        return sharedUserSessionRepository.findById(id);
    }

    @Override
    public UserSession getAndRefreshNN(UUID id, boolean propagate) {
        UserSession userSession = sharedUserSessionRepository.findById(id);

        if (userSession == null)
            throw new NoUserSessionException(id);

        return userSession;
    }

    @Override
    public Stream<UserSessionEntity> getUserSessionEntitiesStream() {
        return Stream.empty();
    }

    @Override
    public Stream<UserSession> getUserSessionsStream() {
        return Stream.empty();
    }

    @Override
    public void add(UserSession session) {
        sharedUserSessionRepository.save(session);
    }

    @Override
    public void remove(UserSession session) {
        // do nothing
    }

    @Override
    public void propagate(UUID id) {
        // do nothing
    }

    @Override
    public Collection<UserSessionEntity> getUserSessionInfo() {
        return Collections.emptyList();
    }

    @Override
    public void killSession(UUID id) {
        // do nothing
    }

    @Override
    public List<UUID> findUserSessionsByAttribute(String attributeName, Object attributeValue) {
        return null;
    }

    @Override
    public int getExpirationTimeoutSec() {
        return expirationTimeout;
    }

    @Override
    public void setExpirationTimeoutSec(int value) {
        this.expirationTimeout = value;
    }

    @Override
    public int getSendTimeoutSec() {
        return sendTimeout;
    }

    @Override
    public void setSendTimeoutSec(int timeout) {
        this.sendTimeout = timeout;
    }

    @Override
    public void processEviction() {
        // do nothing
    }
}
