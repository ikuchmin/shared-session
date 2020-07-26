package ru.udya.sharedsession.client;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.client.ClientUserSession;
import com.haulmont.cuba.security.entity.Access;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.PermissionType;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.group.ConstraintsContainer;
import com.haulmont.cuba.security.role.RoleDefinition;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class SharedUserSessionClientHolder extends ClientUserSession {

    private static final long serialVersionUID = 3938979764755792850L;

    protected UserSession delegate;

    public SharedUserSessionClientHolder(UserSession src) {
        super(src);
        this.delegate = src;
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }

    @Override
    public User getUser() {
        return delegate.getUser();
    }

    @Override
    public void setUser(User user) {
        delegate.setUser(user);
    }

    @Override
    public User getSubstitutedUser() {
        return delegate.getSubstitutedUser();
    }

    @Override
    public void setSubstitutedUser(User substitutedUser) {
        delegate.setSubstitutedUser(substitutedUser);
    }

    @Override
    public User getCurrentOrSubstitutedUser() {
        return delegate.getCurrentOrSubstitutedUser();
    }

    @Override
    public Collection<String> getRoles() {
        return delegate.getRoles();
    }

    @Override
    public Locale getLocale() {
        return delegate.getLocale();
    }

    @Override
    public void setLocale(Locale locale) {
        delegate.setLocale(locale);
    }

    @Override
    @Nullable
    public TimeZone getTimeZone() {
        return delegate.getTimeZone();
    }

    @Override
    public void setTimeZone(TimeZone timeZone) {
        delegate.setTimeZone(timeZone);
    }

    @Override
    public String getAddress() {
        return delegate.getAddress();
    }

    @Override
    public void setAddress(String address) {
        delegate.setAddress(address);
    }

    @Override
    public String getClientInfo() {
        return delegate.getClientInfo();
    }

    @Override
    public void setClientInfo(String clientInfo) {
        delegate.setClientInfo(clientInfo);
    }

    @Override
    public Integer getPermissionValue(PermissionType type,
                                      String target) {
        return delegate.getPermissionValue(type, target);
    }

    @Override
    public Map<String, Integer> getPermissionsByType(
            PermissionType type) {
        return delegate.getPermissionsByType(type);
    }

    @Override
    public boolean isScreenPermitted(String windowAlias) {
        return delegate.isScreenPermitted(windowAlias);
    }

    @Override
    public boolean isEntityOpPermitted(MetaClass metaClass,
                                       EntityOp entityOp) {
        return delegate.isEntityOpPermitted(metaClass, entityOp);
    }

    @Override
    public boolean isEntityAttrPermitted(MetaClass metaClass,
                                         String property,
                                         EntityAttrAccess access) {
        return delegate.isEntityAttrPermitted(metaClass, property, access);
    }

    @Override
    public boolean isSpecificPermitted(String name) {
        return delegate.isSpecificPermitted(name);
    }

    @Override
    public boolean isPermitted(PermissionType type, String target) {
        return delegate.isPermitted(type, target);
    }

    @Override
    public boolean isPermitted(PermissionType type, String target, int value) {
        return delegate.isPermitted(type, target, value);
    }

    @Override
    @Nullable
    public <T> T getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Serializable value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public Collection<String> getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    @Nullable
    public <T> T getLocalAttribute(String name) {
        return delegate.getLocalAttribute(name);
    }

    @Override
    public void removeLocalAttribute(String name) {
        delegate.removeLocalAttribute(name);
    }

    @Override
    public void setLocalAttribute(String name, Object value) {
        delegate.setLocalAttribute(name, value);
    }

    @Override
    public Object setLocalAttributeIfAbsent(String name, Object value) {
        return delegate.setLocalAttributeIfAbsent(name, value);
    }

    @Override
    public Collection<String> getLocalAttributeNames() {
        return delegate.getLocalAttributeNames();
    }

    @Override
    public boolean isSystem() {
        return delegate.isSystem();
    }

    @Override
    public RoleDefinition getJoinedRole() {
        return delegate.getJoinedRole();
    }

    @Override
    public void setJoinedRole(RoleDefinition joinedRole) {
        delegate.setJoinedRole(joinedRole);
    }

    @Override
    public ConstraintsContainer getConstraints() {
        return delegate.getConstraints();
    }

    @Override
    public void setConstraints(ConstraintsContainer constraints) {
        delegate.setConstraints(constraints);
    }

    @Override
    public Access getPermissionUndefinedAccessPolicy() {
        return delegate.getPermissionUndefinedAccessPolicy();
    }

    @Override
    public void setPermissionUndefinedAccessPolicy(
            Access permissionUndefinedAccessPolicy) {
        delegate.setPermissionUndefinedAccessPolicy(permissionUndefinedAccessPolicy);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
