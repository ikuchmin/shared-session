package ru.udya.sharedsession.redis.cache;

import ru.udya.sharedsession.cache.SharedUserSessionCache;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

public interface RedisSharedUserSessionCache
        extends SharedUserSessionCache<RedisSharedUserSession, RedisSharedUserSessionId, String> {
}
