package ru.udya.sharedsession.cache;

import com.haulmont.cuba.security.global.UserSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;

@Component(SharedUserSessionCache.NAME)
public class CoreSharedUserSessionRequestScopeCache
        implements SharedUserSessionCache {

    public static final String SESSION_CACHE_ATTRIBUTE = "shared_session";

    @Override
    public <T extends UserSession> T getUserSessionFromCache(UUID id, String sessionKey) {
        RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            //noinspection unchecked
            return (T) requestAttributes.getAttribute(SESSION_CACHE_ATTRIBUTE,
                    RequestAttributes.SCOPE_REQUEST);
        }

        return null;
    }

    @Override
    public void saveUserSessionInCache(UserSession userSession) {
        RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(SESSION_CACHE_ATTRIBUTE,
                    userSession, RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Override
    public void removeUserSessionFromCache(UserSession userSession) {
        RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.removeAttribute(SESSION_CACHE_ATTRIBUTE,
                    RequestAttributes.SCOPE_REQUEST);
        }
    }
}
