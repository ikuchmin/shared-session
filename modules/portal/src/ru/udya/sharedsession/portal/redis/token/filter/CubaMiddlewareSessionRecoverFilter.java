package ru.udya.sharedsession.portal.redis.token.filter;

import com.google.common.base.Strings;
import com.haulmont.addon.restapi.api.config.RestApiConfig;
import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.app.TrustedClientService;
import com.haulmont.cuba.security.auth.AuthenticationService;
import com.haulmont.cuba.security.auth.TrustedClientCredentials;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component("ss_CubaMiddlewareSessionRecoverFilter")
public class CubaMiddlewareSessionRecoverFilter extends GenericFilterBean {

    private static final Logger log = LoggerFactory.getLogger(CubaMiddlewareSessionRecoverFilter.class);

    protected RestApiConfig restApiConfig;

    protected TrustedClientService trustedClientService;

    protected AuthenticationService authenticationService;

    protected GlobalConfig globalConfig;

    public CubaMiddlewareSessionRecoverFilter(RestApiConfig restApiConfig,
                                              TrustedClientService trustedClientService, AuthenticationService authenticationService, GlobalConfig globalConfig) {
        this.restApiConfig = restApiConfig;
        this.trustedClientService = trustedClientService;
        this.authenticationService = authenticationService;
        this.globalConfig = globalConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        getAuthentication().ifPresent(this::processSession);

        chain.doFilter(request, response);
    }

    protected Optional<OAuth2Authentication> getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2Authentication) {
            return Optional.of((OAuth2Authentication) authentication);
        }
        return Optional.empty();
    }

    /**
     * Tries to find the session associated with the given {@code authentication}. If the session id is in the store and exists then it is set to the
     * {@link SecurityContext}. If the session id is not in the store or the session with the id doesn't exist in the middleware, then the trusted
     * login attempt is performed.
     */
    protected void processSession(OAuth2Authentication authentication) {
        UUID sessionId = null;
        @SuppressWarnings("unchecked")
        Map<String, String> userAuthenticationDetails =
                (Map<String, String>) authentication.getUserAuthentication().getDetails();
        //sessionId parameter was put in the CubaUserAuthenticationProvider
        String sessionIdStr = userAuthenticationDetails.get("sessionId");
        if (!Strings.isNullOrEmpty(sessionIdStr)) {
            sessionId = UUID.fromString(sessionIdStr);
        }

        UserSession session = null;
        if (sessionId != null) {
            try {
                session = trustedClientService.findSession(restApiConfig.getTrustedClientPassword(), sessionId);
            } catch (LoginException e) {
                throw new RuntimeException("Unable to login with trusted client password");
            }
        }

        if (session == null) {
            String username = userAuthenticationDetails.get("username");

            if (Strings.isNullOrEmpty(username)) {
                throw new IllegalStateException("Empty username extracted from user authentication details");
            }

            TrustedClientCredentials credentials = createTrustedClientCredentials(username, extractLocaleFromRequestHeader());
            try {
                session = authenticationService.login(credentials).getSession();
            } catch (LoginException e) {
                throw new OAuth2Exception("Cannot login to the middleware", e);
            }
        }

        setSecurityContext(session);
    }

    protected void setSecurityContext(UserSession session) {
        SecurityContext securityContext = new SecurityContext(session);
        AppContext.setSecurityContext(securityContext);
    }

    protected TrustedClientCredentials createTrustedClientCredentials(String username, Locale locale) {
        TrustedClientCredentials credentials = new TrustedClientCredentials(username,
                restApiConfig.getTrustedClientPassword(), locale);
        credentials.setClientType(ClientType.REST_API);

        HttpServletRequest request = getServletRequestAttributes();
        if (request != null) {
            credentials.setIpAddress(request.getRemoteAddr());
            credentials.setClientInfo(makeClientInfo(request.getHeader(HttpHeaders.USER_AGENT)));
        } else {
            credentials.setClientInfo(makeClientInfo(""));
        }

        credentials.setSecurityScope(restApiConfig.getSecurityScope());

        return credentials;
    }

    @Nullable
    protected HttpServletRequest getServletRequestAttributes() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    protected String makeClientInfo(String userAgent) {
        return String.format("REST API (%s:%s/%s) %s",
                globalConfig.getWebHostName(),
                globalConfig.getWebPort(),
                globalConfig.getWebContextName(),
                StringUtils.trimToEmpty(userAgent));
    }

    protected Locale extractLocaleFromRequestHeader() {
        HttpServletRequest request = getHttpServletRequest();
        Locale locale = null;
        if (!Strings.isNullOrEmpty(request.getHeader(ACCEPT_LANGUAGE))) {
            Locale requestLocale = request.getLocale();

            Map<String, Locale> availableLocales = globalConfig.getAvailableLocales();
            Collection<Locale> locales = availableLocales.values();
            if (!isEmpty(locales) && locales.contains(requestLocale)) {
                locale = requestLocale;
            } else {
                log.debug("Locale {} passed in the Accept-Language header is not supported by the application. It was ignored.", requestLocale);
            }
        }
        return locale;
    }

    protected HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}