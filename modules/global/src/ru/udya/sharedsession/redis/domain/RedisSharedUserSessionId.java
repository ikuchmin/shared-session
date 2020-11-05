package ru.udya.sharedsession.redis.domain;

import ru.udya.sharedsession.domain.SharedUserSessionId;

public interface RedisSharedUserSessionId extends SharedUserSessionId<String> {

    static RedisSharedUserSessionId of(String sharedId) {
        var redisSharedUserSessionId = new RedisSharedUserSessionIdImpl();
        redisSharedUserSessionId.setSharedId(sharedId);

        return redisSharedUserSessionId;
    }

}
