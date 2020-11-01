package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;

import java.util.UUID;

public interface SharedUserPermissionRuntime {

    boolean isPermissionGrantedToUser(String permission, Id<User, UUID> userId);
}
