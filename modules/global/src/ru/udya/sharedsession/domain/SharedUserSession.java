package ru.udya.sharedsession.domain;

import java.io.Serializable;

public interface SharedUserSession {

    Serializable getSharedId();
}
