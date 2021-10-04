package ru.udya.sharedsession.portal.redis.token;

import com.haulmont.cuba.portal.sys.security.RoleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class TestAuthentication implements Authentication {
    private String login;
    private String password;

    private Map<String, String> details;

    public TestAuthentication(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public TestAuthentication(Map<String, String> details) {
        this.details = details;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        GrantedAuthority authority = new RoleGrantedAuthority();
        return Collections.singleton(authority);
    }

    @Override
    public Object getCredentials() {
        return login;
    }

    @Override
    public Object getDetails() {
        return details;
    }

    @Override
    public Object getPrincipal() {
        return password;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestAuthentication that = (TestAuthentication) o;
        return Objects.equals(login, that.login) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login, password);
    }
}
