package ru.udya.sharedsession.redis.domain;

import com.haulmont.cuba.security.global.UserSession;
import ru.udya.sharedsession.domain.SharedUserSession;

public class RedisSharedUserSession
        extends RedisSharedUserSessionId
        implements SharedUserSession {

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
    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

    @Override
    public String toString() {
        return "RedisSharedUserSession{" +
               "sharedId='" + sharedId + '\'' +
               '}';
    }
}
