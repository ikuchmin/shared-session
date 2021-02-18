package ru.udya.sharedsession.serialization;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.sys.serialization.Serialization;
import com.haulmont.cuba.core.sys.serialization.StandardSerialization;
import com.haulmont.cuba.security.auth.SimpleAuthenticationDetails;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.remoting.support.RemoteInvocationResult;
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

        Object serializedObject = preProcessSerializedObject(object);

        super.serialize(serializedObject, os);
    }

    protected Object preProcessSerializedObject(Object object) {
        Object serializedObject = object;
        if (object instanceof RemoteInvocationResult) {
            RemoteInvocationResult remoteInvocationResult = ((RemoteInvocationResult) object);
            Object value = remoteInvocationResult.getValue();
            Throwable exception = remoteInvocationResult.getException();

            if (exception != null) {
                return serializedObject;
            }

            Object internalObject = value;
            if (value instanceof SharedUserSessionId) {
                internalObject = createSharedUserSessionHolder((SharedUserSessionId<?>) value);
            }

            if (value instanceof SimpleAuthenticationDetails) {
                UserSession userSession = ((SimpleAuthenticationDetails) value).getSession();

                if (userSession instanceof SharedUserSessionId) {
                    SharedUserSessionHolder sharedUserSessionHolder =
                            createSharedUserSessionHolder((SharedUserSessionId<?>) userSession);

                    internalObject = new SimpleAuthenticationDetails(sharedUserSessionHolder);
                }
            }

            serializedObject = new RemoteInvocationResult(internalObject);
        }

        if (object instanceof SimpleAuthenticationDetails) {
            UserSession userSession = ((SimpleAuthenticationDetails) object).getSession();

            if (userSession instanceof SharedUserSessionId) {
                SharedUserSessionHolder sharedUserSessionHolder =
                        createSharedUserSessionHolder((SharedUserSessionId<?>) userSession);

                serializedObject = new SimpleAuthenticationDetails(sharedUserSessionHolder);
            }
        }

        if (object instanceof SharedUserSessionId) {
            serializedObject = createSharedUserSessionHolder((SharedUserSessionId<?>) object);
        }
        return serializedObject;
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

        desObject = postProcessDeserializedObject(desObject);

        return desObject;
    }

    private Object postProcessDeserializedObject(Object desObject) {
        if (desObject instanceof RemoteInvocationResult) {
            RemoteInvocationResult remoteInvocationResult = ((RemoteInvocationResult) desObject);
            Object value = remoteInvocationResult.getValue();
            Throwable exception = remoteInvocationResult.getException();

            if (exception != null) {
                return desObject;
            }

            Object internalObject = value;
            if (value instanceof SharedUserSessionHolder) {
                internalObject = fetchSharedUserSessionByHolder((SharedUserSessionHolder) value);
            }

            if (value instanceof SimpleAuthenticationDetails) {
                UserSession userSession = ((SimpleAuthenticationDetails) value).getSession();

                if (userSession instanceof SharedUserSessionHolder) {
                    UserSession desSharedUserSession =
                            fetchSharedUserSessionByHolder((SharedUserSessionHolder) userSession);

                    internalObject = new SimpleAuthenticationDetails(desSharedUserSession);
                }
            }

            desObject = new RemoteInvocationResult(internalObject);
        }

        if (desObject instanceof SimpleAuthenticationDetails) {
            UserSession userSession = ((SimpleAuthenticationDetails) desObject).getSession();

            if (userSession instanceof SharedUserSessionHolder) {
                UserSession desSharedUserSession =
                        fetchSharedUserSessionByHolder((SharedUserSessionHolder) userSession);

                desObject = new SimpleAuthenticationDetails(desSharedUserSession);
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
