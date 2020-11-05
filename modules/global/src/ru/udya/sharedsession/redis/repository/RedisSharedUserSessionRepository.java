package ru.udya.sharedsession.redis.repository;

import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import java.util.function.Consumer;

public interface RedisSharedUserSessionRepository
        extends SharedUserSessionRepository<RedisSharedUserSession, RedisSharedUserSessionId, String> {

    public static final String KEY_PREFIX = "shared:session";

    // shared:session:userId:sessionId
    public static final String KEY_PATTERN = KEY_PREFIX + ":" + "%s:%s";

    RedisSharedUserSession updateByFn(RedisSharedUserSessionId redisSharedUserSessionId,
                                      Consumer<RedisSharedUserSession> updateFn);
}
