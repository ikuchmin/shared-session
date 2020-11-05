package ru.udya.sharedsession.domain;

import com.haulmont.cuba.security.global.UserSession;

import java.io.Serializable;

public interface SharedUserSession<ID extends Serializable> extends SharedUserSessionId<ID> {

    UserSession getCubaUserSession();
}
