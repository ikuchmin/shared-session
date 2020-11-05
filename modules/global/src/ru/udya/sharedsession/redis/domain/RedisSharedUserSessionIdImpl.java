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
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        RedisSharedUserSessionIdImpl that = (RedisSharedUserSessionIdImpl) o;

        return sharedId.equals(that.sharedId);
    }

    @Override
    public int hashCode() {
        return sharedId.hashCode();
    }

    @Override
    public String toString() {
        return "sharedId='" + sharedId;
    }
}
