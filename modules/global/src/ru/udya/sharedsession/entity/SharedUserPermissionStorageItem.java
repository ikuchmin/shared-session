package ru.udya.sharedsession.entity;

import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "SS_SHARED_USER_PERMISSION_STORAGE_ITEM")
@Entity(name = "ss_SharedUserPermissionStorageItem")
public class SharedUserPermissionStorageItem extends BaseUuidEntity {

    private static final long serialVersionUID = - 2718588362196073304L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID")
    protected User user;

    @Column(name = "PERMISSION", unique = true, nullable = false)
    protected String permission;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
