package ru.udya.sharedsession.cache;

import com.haulmont.cuba.security.global.UserSession;

import java.util.UUID;
import java.util.function.Function;

public interface SharedUserSessionCache {

    String NAME = "ss_SharedUserSessionCache";

    <T extends UserSession> T getFromCacheBySessionKey(
            String sessionKey, Function<String, T> getBySessionKeyId);

    void saveInCache(String sessionKey, UserSession userSession);

    void removeFromCache(String sessionKey);
}
