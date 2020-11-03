package ru.udya.sharedsession.permission.runtime;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.User;
import ru.udya.sharedsession.permission.domain.SharedUserPermission;

import java.util.UUID;

public interface SharedUserPermissionRuntime {

    String NAME = "ss_SharedUserPermissionRuntime";

    boolean isPermissionGrantedToUser(Id<User, UUID> userId, SharedUserPermission permission);
}
