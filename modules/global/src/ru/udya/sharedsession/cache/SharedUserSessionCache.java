package ru.udya.sharedsession.cache;

import com.haulmont.cuba.security.global.UserSession;

import java.util.UUID;

public interface SharedUserSessionCache {

    String NAME = "ss_SharedUserSessionCache";

    <T extends UserSession> T getUserSessionFromCacheById(UUID id);

    <T extends UserSession> T getUserSessionFromCacheBySessionKey(String sessionKey);

    void saveUserSessionInCache(UserSession userSession);

    void removeUserSessionFromCache(UserSession userSession);
}
