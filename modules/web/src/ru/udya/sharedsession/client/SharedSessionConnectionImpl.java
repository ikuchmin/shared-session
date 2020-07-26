package ru.udya.sharedsession.client;

import com.haulmont.cuba.client.ClientUserSession;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.security.ConnectionImpl;

public class SharedSessionConnectionImpl  extends ConnectionImpl {

    @Override
    protected ClientUserSession createSession(UserSession userSession) {
        return new SharedUserSessionClientHolder(userSession);
    }
}
