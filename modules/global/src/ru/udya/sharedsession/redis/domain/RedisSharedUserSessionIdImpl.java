package ru.udya.sharedsession.redis.domain;

public class RedisSharedUserSessionIdImpl implements RedisSharedUserSessionId {
    
    protected String sharedId;

    @Override
    public String getSharedId() {
        return sharedId;
    }

    public void setSharedId(String sharedId) {
        this.sharedId = sharedId;
    }

    @Override
    public String toString() {
        return "sharedId='" + sharedId;
    }
}
