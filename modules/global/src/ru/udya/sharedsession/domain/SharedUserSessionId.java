package ru.udya.sharedsession.domain;

import java.io.Serializable;

public interface SharedUserSessionId<ID extends Serializable> {

    ID getSharedId();

}
