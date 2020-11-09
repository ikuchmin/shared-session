package ru.udya.sharedsession.serialization;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.sys.serialization.Serialization;
import com.haulmont.cuba.core.sys.serialization.StandardSerialization;
import com.haulmont.cuba.security.auth.SimpleAuthenticationDetails;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.remoting.support.RemoteInvocationResult;
import ru.udya.sharedsession.domain.SharedUserSession;
import ru.udya.sharedsession.domain.SharedUserSessionId;
import ru.udya.sharedsession.repository.SharedUserSessionRuntimeAdapter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class SharedUserSessionStandardSerialization
        extends StandardSerialization
        implements Serialization {

    @Override
    public void serialize(Object object, OutputStream os) {

        Object serializedObject = object;
        if (object instanceof RemoteInvocationResult) {
            Object value = ((RemoteInvocationResult) object).getValue();

            if (value instanceof SharedUserSessionId) {
                Object sharedUserSessionHolder =
                        createSharedUserSessionHolder((SharedUserSessionId<?>) value);

                serializedObject = new RemoteInvocationResult(sharedUserSessionHolder);
            }

            if (value instanceof SimpleAuthenticationDetails) {
                UserSession userSession = ((SimpleAuthenticationDetails) value).getSession();

                if (userSession instanceof SharedUserSessionId) {
                    SharedUserSessionHolder sharedUserSessionHolder =
                            createSharedUserSessionHolder((SharedUserSessionId<?>) userSession);

                    serializedObject = new RemoteInvocationResult(
                            new SimpleAuthenticationDetails(sharedUserSessionHolder));
                }
            }
        }

        if (object instanceof SharedUserSessionId) {
            serializedObject = createSharedUserSessionHolder((SharedUserSessionId<?>) object);
        }

        super.serialize(serializedObject, os);
    }

    protected SharedUserSessionHolder createSharedUserSessionHolder(SharedUserSessionId<?> sharedUserSession) {
        Serializable sessionId = sharedUserSession.getSharedId();

        SharedUserSessionHolder userSessionHolder =
                new SharedUserSessionHolder();

        userSessionHolder.setSharedId(sessionId);

        return userSessionHolder;
    }

    @Override
    public Object deserialize(InputStream is) {
        Object desObject = super.deserialize(is);

        if (desObject instanceof RemoteInvocationResult) {
            Object value = ((RemoteInvocationResult) desObject).getValue();

            if (value instanceof SharedUserSessionHolder) {
                Object desSharedUserSession =
                        fetchSharedUserSessionByHolder((SharedUserSessionHolder) value);

                desObject = new RemoteInvocationResult(desSharedUserSession);
            }

            if (value instanceof SimpleAuthenticationDetails) {
                UserSession userSession = ((SimpleAuthenticationDetails) value).getSession();

                if (userSession instanceof SharedUserSessionHolder) {
                    UserSession desSharedUserSession =
                            fetchSharedUserSessionByHolder((SharedUserSessionHolder) userSession);

                    desObject = new RemoteInvocationResult(new SimpleAuthenticationDetails(desSharedUserSession));
                }
            }
        }

        if (desObject instanceof SharedUserSessionHolder) {
            desObject = fetchSharedUserSessionByHolder((SharedUserSessionHolder) desObject);
        }
        return desObject;
    }

    public UserSession fetchSharedUserSessionByHolder(SharedUserSessionHolder sharedUserSessionHolder) {

        Serializable sessionId = sharedUserSessionHolder.getSharedId();

        // we don't cache the result because it is fast as is
        // there is we exploit idea that serialization is performed into initialized spring context
        SharedUserSessionRuntimeAdapter sharedUserSessionRepository =
                AppBeans.get(SharedUserSessionRuntimeAdapter.class);

        return sharedUserSessionRepository.findBySharedId(sessionId);
    }
}
