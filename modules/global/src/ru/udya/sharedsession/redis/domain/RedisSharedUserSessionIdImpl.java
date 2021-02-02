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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof RedisSharedUserSessionId)) return false;

        RedisSharedUserSessionId that = (RedisSharedUserSessionId) o;

        return getSharedId().equals(that.getSharedId());
    }

    @Override
    public int hashCode() {
        return getSharedId().hashCode();
    }

    @Override
    public String toString() {
        return "sharedId='" + sharedId;
    }
}
