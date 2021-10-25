package ru.udya.sharedsession.portal.redis.token;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import ru.udya.sharedsession.portal.redis.token.codec.RedisOAuth2AccessTokenCodec;
import ru.udya.sharedsession.portal.redis.token.codec.RedisOAuth2AuthenticationCodec;
import ru.udya.sharedsession.portal.redis.token.codec.RedisOAuth2RefreshTokenCodec;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

public class RedisSharedTokenStore implements TokenStore {

    protected RedisClient redisClient;

    protected RedisCommands<String, OAuth2AccessToken> accessTokenCommand;
    protected RedisCommands<String, OAuth2RefreshToken> refreshTokenCommand;
    protected RedisCommands<String, OAuth2Authentication> authenticationCommand;
    protected RedisCommands<String, String> refreshCommand;

    private final RedisSharedTokenKeyTool keyHelper = new RedisSharedTokenKeyTool();

    public RedisSharedTokenStore(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        accessTokenCommand = connect(new RedisOAuth2AccessTokenCodec());

        refreshTokenCommand = connect(new RedisOAuth2RefreshTokenCodec());

        authenticationCommand = connect(new RedisOAuth2AuthenticationCodec());

        refreshCommand = connect(StringCodec.UTF8);
    }

    protected <K, V> RedisCommands<K, V> connect(RedisCodec<K, V> codec) {
        return redisClient.connect(codec).sync();
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void close() {
        accessTokenCommand.getStatefulConnection().close();
        refreshTokenCommand.getStatefulConnection().close();
        authenticationCommand.getStatefulConnection().close();
        refreshCommand.getStatefulConnection().close();
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        OAuth2AccessToken accessToken = accessTokenCommand.get(keyHelper.createAuthToAccessKey(authentication));
        if (accessToken != null) {
            OAuth2Authentication storedAuthentication = readAuthentication(accessToken.getValue());
            if (keyHelper.areNotEqual(authentication, storedAuthentication)) {
                storeAccessToken(accessToken, authentication);
            }
        }
        return accessToken;
    }

    @Override
    public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
        return readAuthentication(token.getValue());
    }

    @Override
    public OAuth2Authentication readAuthentication(String token) {
        return authenticationCommand.get(keyHelper.createAuthKey(token));
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
        return authenticationCommand.get(keyHelper.createRefreshAuthKey(token));
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

            setKeysExpirationIfNecessary(refreshToken, refreshToAccessKey, accessToRefreshKey);
        }
    }

    private Optional<Integer> extractExpirationSeconds(OAuth2RefreshToken refreshToken) {
        if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
            ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) refreshToken;
            Date expiration = expiringRefreshToken.getExpiration();
            if (expiration != null) {
                return Optional.of(Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L).intValue());
            }
        }
        return Optional.empty();
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken accessToken) {
        removeAccessToken(accessToken.getValue());
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        return accessTokenCommand.get(keyHelper.createAccess(tokenValue));
    }

    public void removeAccessToken(String tokenValue) {
        String accessKey = keyHelper.createAccess(tokenValue);
        String authKey = keyHelper.createAuthKey(tokenValue);
        String accessToRefreshKey = keyHelper.createAccessToRefreshKey(tokenValue);

        accessTokenCommand.del(accessKey);
        refreshCommand.del(accessToRefreshKey);

        OAuth2Authentication authentication = authenticationCommand.get(authKey);
        authenticationCommand.del(authKey);

        if (authentication != null) {
            accessTokenCommand.del(keyHelper.createAuthToAccessKey(authentication));
        }
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        String refreshKey = keyHelper.createRefreshKey(refreshToken);
        String refreshAuthKey = keyHelper.createRefreshAuthKey(refreshToken);

        refreshTokenCommand.set(refreshKey, refreshToken);
        authenticationCommand.set(refreshAuthKey, authentication);

        setKeysExpirationIfNecessary(refreshToken, refreshKey, refreshAuthKey);

    }

    private void setKeysExpirationIfNecessary(OAuth2RefreshToken refreshToken, String refreshKey, String refreshAuthKey) {
        extractExpirationSeconds(refreshToken).ifPresent(seconds -> {
            refreshTokenCommand.expire(refreshKey, seconds);
            authenticationCommand.expire(refreshAuthKey, seconds);
        });
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
        String refreshToAccessKey = keyHelper.createRefreshToAccessKey(tokenValue);
        refreshTokenCommand.del(refreshKey);
        authenticationCommand.del(refreshAuthKey);
        String accessToken = refreshCommand.getdel(refreshToAccessKey);
        refreshCommand.del(keyHelper.createAccessToRefreshKey(accessToken));
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        String key = keyHelper.createRefreshToAccessKey(refreshToken);
        String accessToken = refreshCommand.getdel(key);
        if (accessToken != null) {
            removeAccessToken(accessToken);
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
}
