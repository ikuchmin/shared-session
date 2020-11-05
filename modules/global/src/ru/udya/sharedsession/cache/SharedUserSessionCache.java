package ru.udya.sharedsession.cache;

import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.domain.SharedUserSessionId;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.function.Function;

public interface SharedUserSessionCache<S extends SharedUserSession<ID>,
        SID extends SharedUserSessionId<ID>, ID extends Serializable> {

    String NAME = "ss_SharedUserSessionCache";

    @Nullable
    S getFromCacheBySharedId(SID sharedUserSessionId);

    S getFromCacheBySharedId(SID sharedUserSessionId,
                             Function<SID, S> loaderIfAbsent);

    void saveInCache(RedisSharedUserSession redisSharedUserSession);

    void removeFromCache(RedisSharedUserSessionId sessionKey);
}
