package ru.udya.sharedsession.serialization;

import com.haulmont.cuba.security.global.UserSession;

import java.io.Serializable;
import java.util.UUID;

public class SharedUserSessionHolder extends UserSession implements Serializable {

    private static final long serialVersionUID = - 7410104013973120477L;

    protected UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
