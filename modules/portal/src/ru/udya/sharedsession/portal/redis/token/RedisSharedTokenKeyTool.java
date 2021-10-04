package ru.udya.sharedsession.portal.redis.token;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;

public class RedisSharedTokenKeyTool {

    private static final String KEY_SHARED_PREFIX = "shared:token-store:";

    private static final String AUTH_TO_ACCESS = KEY_SHARED_PREFIX + "auth_to_access:%s";
    private static final String REFRESH_AUTH = KEY_SHARED_PREFIX + "refresh_auth:%s";
    private static final String AUTH = KEY_SHARED_PREFIX + "auth:%s";
    private static final String ACCESS = KEY_SHARED_PREFIX + "access:%s";
    private static final String ACCESS_TO_REFRESH = KEY_SHARED_PREFIX + "access_to_refresh:%s";
    private static final String REFRESH_TO_ACCESS = KEY_SHARED_PREFIX + "refresh_to_access:%s";
    private static final String REFRESH = KEY_SHARED_PREFIX + "refresh:%s";

    private final AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

    public String createAuthKey(OAuth2AccessToken tokenValue) {
        return createAuthKey(tokenValue.getValue());
    }

    public String createAuthKey(String tokenValue) {
        return String.format(AUTH, tokenValue);
    }

    public String createAuthToAccessKey(OAuth2Authentication authentication) {
        String key = authenticationKeyGenerator.extractKey(authentication);
        return String.format(AUTH_TO_ACCESS, key);
    }

    public String createRefreshTokenKey(OAuth2RefreshToken refreshToken) {
        return String.format(REFRESH_TO_ACCESS, refreshToken.getValue());
    }

    public String createRefreshKey(OAuth2RefreshToken refreshToken) {
        return createRefreshKey(refreshToken.getValue());
    }

    public String createRefreshKey(String refreshToken) {
        return String.format(REFRESH, refreshToken);
    }

    public String createRefreshAuthKey(OAuth2RefreshToken refreshToken) {
        return createRefreshAuthKey(refreshToken.getValue());
    }

    public String createRefreshAuthKey(String refreshToken) {
        return String.format(REFRESH_AUTH, refreshToken);
    }

    public String createAccessToRefreshKey(OAuth2AccessToken token) {
        return createAccessToRefreshKey(token.getValue());
    }

    public String createAccessToRefreshKey(String token) {
        return String.format(ACCESS_TO_REFRESH, token);
    }

    public String createRefreshToAccessKey(OAuth2RefreshToken token) {
        return createRefreshToAccessKey(token.getValue());
    }

    public String createRefreshToAccessKey(String token) {
        return String.format(REFRESH_TO_ACCESS, token);
    }

    public String createAccess(OAuth2AccessToken token) {
        return createAccess(token.getValue());
    }

    public String createAccess(String token) {
        return String.format(ACCESS, token);
    }

    public boolean areNotEqual(OAuth2Authentication auth, OAuth2Authentication otherAuth) {
        String key = authenticationKeyGenerator.extractKey(auth);
        return otherAuth == null || !key.equals(authenticationKeyGenerator.extractKey(otherAuth));
    }
}
