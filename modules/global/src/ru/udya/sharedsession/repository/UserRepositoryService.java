package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component(UserRepositoryService.NAME)
public interface UserRepositoryService {

    String NAME = "ss_UserRepositoryService";

    List<User> findAllHavingRole(Id<Role, UUID> roleId);
}
