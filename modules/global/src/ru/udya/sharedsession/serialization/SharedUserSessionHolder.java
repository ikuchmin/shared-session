package ru.udya.sharedsession.serialization;

import com.haulmont.cuba.security.global.UserSession;

import java.io.Serializable;

public class SharedUserSessionHolder extends UserSession implements Serializable {

    private static final long serialVersionUID = - 7410104013973120477L;

    protected Serializable sharedId;

    public Serializable getSharedId() {
        return sharedId;
    }

    public void setSharedId(Serializable sharedId) {
        this.sharedId = sharedId;
    }
}
