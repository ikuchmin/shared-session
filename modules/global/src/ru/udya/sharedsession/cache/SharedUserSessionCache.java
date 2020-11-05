package ru.udya.sharedsession.cache;

import com.haulmont.cuba.security.global.UserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;

import java.util.function.Function;

public interface SharedUserSessionCache {

    String NAME = "ss_SharedUserSessionCache";

    <T extends UserSession> T getFromCacheBySessionKey(
            String sessionKey, Function<String, T> getBySessionKeyId);

    void saveInCache(RedisSharedUserSession redisSharedUserSession);

    void removeFromCache(String sessionKey);
}
