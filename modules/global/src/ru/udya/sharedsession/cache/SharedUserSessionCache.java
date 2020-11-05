package ru.udya.sharedsession.cache;

import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;

import java.io.Serializable;
import java.util.function.Function;

public interface SharedUserSessionCache<T extends SharedUserSession<ID>, ID extends Serializable> {

    String NAME = "ss_SharedUserSessionCache";

    T getFromCacheBySessionKey(String sessionKey,
                               Function<String, T> getBySessionKeyId);

    void saveInCache(RedisSharedUserSession redisSharedUserSession);

    void removeFromCache(String sessionKey);
}
