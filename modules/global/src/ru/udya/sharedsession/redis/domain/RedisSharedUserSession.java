package ru.udya.sharedsession.redis.domain;

import com.haulmont.cuba.security.global.UserSession;
import ru.udya.sharedsession.domain.SharedUserSession;

public class RedisSharedUserSession
        implements SharedUserSession<String>, RedisSharedUserSessionId {

    protected String sharedId;
    protected UserSession userSession;

    public static RedisSharedUserSession of(RedisSharedUserSessionId sharedUserSessionId,
                                            UserSession userSession) {

        var redisSharedUserSession = new RedisSharedUserSession();
        redisSharedUserSession.setSharedId(sharedUserSessionId.getSharedId());
        redisSharedUserSession.setUserSession(userSession);

        return redisSharedUserSession;
    }


    public static RedisSharedUserSession of(String sharedId, UserSession userSession) {
        var redisSharedUserSession = new RedisSharedUserSession();
        redisSharedUserSession.setSharedId(sharedId);
        redisSharedUserSession.setUserSession(userSession);

        return redisSharedUserSession;
    }



    @Override
    public String getSharedId() {
        return sharedId;
    }

    public void setSharedId(String sharedId) {
        this.sharedId = sharedId;
    }

    @Override
    public UserSession getCubaUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

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
        return "RedisSharedUserSession{" +
                "sharedId='" + sharedId + '\'' +
                ", userSession=" + userSession +
                '}';
    }
}
