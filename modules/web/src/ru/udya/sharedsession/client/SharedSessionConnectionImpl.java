package ru.udya.sharedsession.client;

import com.haulmont.cuba.client.ClientUserSession;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.security.ConnectionImpl;
import org.springframework.stereotype.Component;

@Component(SharedSessionConnectionImpl.NAME)
public class SharedSessionConnectionImpl extends ConnectionImpl {

    public static final String NAME = "ss_SharedSessionConnectionImpl";

    @Override
    protected ClientUserSession createSession(UserSession userSession) {
        return new SharedUserSessionClientHolder(userSession);
    }
}
