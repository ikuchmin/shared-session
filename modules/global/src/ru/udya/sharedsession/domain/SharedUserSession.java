package ru.udya.sharedsession.domain;

import com.haulmont.cuba.security.global.UserSession;

public interface SharedUserSession extends SharedUserSessionId {

    UserSession getUserSession();
}
