package ru.udya.sharedsession.redis.domain;

import ru.udya.sharedsession.domain.SharedUserSessionId;

public class RedisSharedUserSessionId implements SharedUserSessionId {

    protected String sharedId;

    public static RedisSharedUserSessionId of(String sharedId) {
        var redisSharedUserSessionId = new RedisSharedUserSessionId();
        redisSharedUserSessionId.setSharedId(sharedId);

        return redisSharedUserSessionId;
    }

    @Override
    public String getSharedId() {
        return sharedId;
    }

    public void setSharedId(String sharedId) {
        this.sharedId = sharedId;
    }
}
