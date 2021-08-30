package ru.udya.sharedsession.portal.redis.token;

import com.google.common.base.Strings;
import com.haulmont.addon.restapi.api.common.RestTokenMasker;
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
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.udya.sharedsession.portal.redis.token.codec.RedisOAuth2AccessTokenCodec;
import ru.udya.sharedsession.portal.redis.token.codec.RedisOAuth2AuthenticationCodec;
import ru.udya.sharedsession.portal.redis.token.codec.RedisOAuth2RefreshTokenCodec;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RedisSharedTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisSharedTokenStore.class);

    @Inject
    protected AuthenticationService authenticationService;

    @Inject
    protected TrustedClientService trustedClientService;

    @Inject
    protected RestTokenMasker tokenMasker;

    @Inject
    protected GlobalConfig globalConfig;

    @Inject
    protected RestApiConfig restApiConfig;

    @Inject
    protected RedisClient redisClient;

    protected RedisCommands<String, OAuth2AccessToken> accessTokenCommand;

    protected RedisCommands<String, OAuth2RefreshToken> refreshTokenCommand;

    protected RedisCommands<String, OAuth2Authentication> authenticationCommand;

    protected RedisCommands<String, String> refreshCommand;

    private final RedisSharedTokenKeyTool keyHelper = new RedisSharedTokenKeyTool();

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        accessTokenCommand = redisClient.connect(new RedisOAuth2AccessTokenCodec()).sync();

        refreshTokenCommand = redisClient.connect(new RedisOAuth2RefreshTokenCodec()).sync();

        authenticationCommand = redisClient.connect(new RedisOAuth2AuthenticationCodec()).sync();

        refreshCommand = redisClient.connect(new StringCodec(StandardCharsets.UTF_8)).sync();
    }

    @PreDestroy
    public void close() {
        accessTokenCommand.getStatefulConnection().close();
        refreshTokenCommand.getStatefulConnection().close();
        authenticationCommand.getStatefulConnection().close();
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        OAuth2AccessToken accessToken = accessTokenCommand.get(keyHelper.createAuthToAccessKey(authentication));
        if (accessToken != null) {
            OAuth2Authentication storedAuthentication = this.readAuthentication(accessToken.getValue());
            if (keyHelper.areNotEqual(authentication, storedAuthentication)) {
                this.storeAccessToken(accessToken, authentication);
            }
        }
        return accessToken;
    }

    @Override
    public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
        return this.readAuthentication(token.getValue());
    }

    @Override
    public OAuth2Authentication readAuthentication(String token) {
        OAuth2Authentication authentication = authenticationCommand.get(keyHelper.createAuthKey(token));

        if (authentication != null) {
            processSession(authentication, token);
        }
        return authentication;
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
        return authenticationCommand.get(keyHelper.createRefreshAuthKey(token));
    }

    /**
     * Tries to find the session associated with the given {@code authentication}. If the session id is in the store and exists then it is set to the
     * {@link SecurityContext}. If the session id is not in the store or the session with the id doesn't exist in the middleware, then the trusted
     * login attempt is performed.
     */
    protected void processSession(OAuth2Authentication authentication, String tokenValue) {
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
            log.debug("New session created for token '{}' since the original session has been expired", tokenMasker.maskToken(tokenValue));
        }

        if (session != null) {
            AppContext.setSecurityContext(new SecurityContext(session));
        }
    }

    protected TrustedClientCredentials createTrustedClientCredentials(String username, Locale locale) {
        TrustedClientCredentials credentials = new TrustedClientCredentials(username,
                restApiConfig.getTrustedClientPassword(), locale);
        credentials.setClientType(ClientType.REST_API);

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            credentials.setIpAddress(request.getRemoteAddr());
            credentials.setClientInfo(makeClientInfo(request.getHeader(HttpHeaders.USER_AGENT)));
        } else {
            credentials.setClientInfo(makeClientInfo(""));
        }

        credentials.setSecurityScope(restApiConfig.getSecurityScope());

        return credentials;
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        String accessKey = keyHelper.createAccess(token);
        String authKey = keyHelper.createAuthKey(token);
        String authToAccessKey = keyHelper.createAuthToAccessKey(authentication);

        accessTokenCommand.set(accessKey, token);
        authenticationCommand.set(authKey, authentication);
        accessTokenCommand.set(authToAccessKey, token);

        if (token.getExpiration() != null) {
            int seconds = token.getExpiresIn();
            accessTokenCommand.expire(accessKey, seconds);
            authenticationCommand.expire(authKey, seconds);
            accessTokenCommand.expire(authToAccessKey, seconds);
        }

        OAuth2RefreshToken refreshToken = token.getRefreshToken();
        if (refreshToken != null && refreshToken.getValue() != null) {
            String refreshToAccessKey = keyHelper.createRefreshTokenKey(refreshToken);
            String accessToRefreshKey = keyHelper.createAccessToRefreshKey(token);

            refreshCommand.set(refreshToAccessKey, token.getValue());
            refreshCommand.set(accessToRefreshKey, refreshToken.getValue());

            if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
                ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) refreshToken;
                Date expiration = expiringRefreshToken.getExpiration();
                if (expiration != null) {
                    int seconds = Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L).intValue();
                    refreshCommand.expire(refreshToAccessKey, seconds);
                    refreshCommand.expire(accessToRefreshKey, seconds);
                }
            }
        }
    }

    public Locale extractLocaleFromRequestHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Locale locale = null;
        if (!Strings.isNullOrEmpty(request.getHeader(org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE))) {
            Locale requestLocale = request.getLocale();

            Map<String, Locale> availableLocales = globalConfig.getAvailableLocales();
            if (availableLocales.values().contains(requestLocale)) {
                locale = requestLocale;
            } else {
                log.debug("Locale {} passed in the Accept-Language header is not supported by the application. It was ignored.", requestLocale);
            }
        }
        return locale;
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken accessToken) {
        this.removeAccessToken(accessToken.getValue());
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        return accessTokenCommand.get(keyHelper.createAccess(tokenValue));
    }

    public void removeAccessToken(String tokenValue) {
        String accessKey = keyHelper.createAccess(tokenValue);
        String authKey = keyHelper.createAuthKey(tokenValue);
        String accessToRefreshKey = keyHelper.createAccessToRefreshKey(tokenValue);

        OAuth2Authentication authentication = authenticationCommand.get(authKey);

        accessTokenCommand.del(accessKey);
        refreshCommand.del(accessToRefreshKey);
        authenticationCommand.del(authKey);

        if (authentication != null) {
            accessTokenCommand.del(keyHelper.createAuthToAccessKey(authentication),
                    keyHelper.createAuthKey(authentication));
        }
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        String refreshKey = keyHelper.createRefreshKey(refreshToken);
        String refreshAuthKey = keyHelper.createRefreshAuthKey(refreshToken);

        refreshTokenCommand.set(refreshKey, refreshToken);
        authenticationCommand.set(refreshAuthKey, authentication);


        if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
            ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) refreshToken;
            Date expiration = expiringRefreshToken.getExpiration();
            if (expiration != null) {
                int seconds = Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L).intValue();
                refreshTokenCommand.expire(refreshKey, seconds);
                authenticationCommand.expire(refreshAuthKey, seconds);
            }
        }
    }

    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        String key = keyHelper.createRefreshKey(tokenValue);
        return refreshTokenCommand.get(key);
    }

    @Override
    public void removeRefreshToken(OAuth2RefreshToken refreshToken) {
        this.removeRefreshToken(refreshToken.getValue());
    }

    public void removeRefreshToken(String tokenValue) {
        String refreshKey = keyHelper.createRefreshKey(tokenValue);
        String refreshAuthKey = keyHelper.createRefreshAuthKey(tokenValue);
        String refresh2AccessKey = keyHelper.createRefreshToAccessKey(tokenValue);
        String access2RefreshKey = keyHelper.createAccessToRefreshKey(tokenValue);
        refreshTokenCommand.del(refreshKey);
        authenticationCommand.del(refreshAuthKey);
        refreshCommand.del(refresh2AccessKey, access2RefreshKey);
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        String key = keyHelper.createRefreshToAccessKey(refreshToken);
        String accessToken = refreshCommand.getdel(key);
        if (accessToken != null) {
            this.removeAccessToken(accessToken);
        }
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
        throw new UnsupportedOperationException();
    }

    protected String makeClientInfo(String userAgent) {
        String serverInfo = String.format("REST API (%s:%s/%s) %s",
                globalConfig.getWebHostName(),
                globalConfig.getWebPort(),
                globalConfig.getWebContextName(),
                StringUtils.trimToEmpty(userAgent));

        return serverInfo;
    }
}
