package ru.udya.sharedsession.service;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.UUID;

public interface SharedUserPermissionRuntime {

    String NAME = "ss_SharedUserPermissionRuntime";

    boolean isPermissionGrantedToUser(SharedUserPermission permission, Id<User, UUID> userId);
}
