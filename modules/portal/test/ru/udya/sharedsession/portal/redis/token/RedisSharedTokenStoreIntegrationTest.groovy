package ru.udya.sharedsession.portal.redis.token

import com.haulmont.cuba.core.global.AppBeans
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken
import org.springframework.security.oauth2.common.OAuth2RefreshToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import ru.udya.sharedsession.portal.testsupport.PortalIntegrationSpecification

import java.time.LocalDate
import java.time.ZoneId

class RedisSharedTokenStoreIntegrationTest extends PortalIntegrationSpecification {
    RedisSharedTokenStore testClass

    void setup() {
        testClass = AppBeans.get(RedisSharedTokenStore)
    }

    def "check that an access token is saved"() {
        OAuth2Authentication auth = createAuthentication("test", "test")

        def tokenValue = UUID.randomUUID().toString()
        def token = new DefaultOAuth2AccessToken(tokenValue)

        when: "try to save a default access token"
        testClass.storeAccessToken(token, auth)

        def authToAccessToken = testClass.getAccessToken(auth)
        def authByAccessToken = testClass.readAuthentication(token)
        def accessToken = testClass.readAccessToken(tokenValue)

        then:
        authToAccessToken != null
        authByAccessToken != null
        accessToken != null
        authByAccessToken == auth
        authToAccessToken == accessToken
        authToAccessToken.getValue() == tokenValue

        when: "try to save a default access token with an expiration property"
        token.setExpiration(createDate(5))
        testClass.storeAccessToken(token, auth)

        authToAccessToken = testClass.getAccessToken(auth)
        authByAccessToken = testClass.readAuthentication(tokenValue)
        accessToken = testClass.readAccessToken(tokenValue)

        then:
        authToAccessToken != null
        authByAccessToken != null
        accessToken != null
        authToAccessToken.getValue() == tokenValue

        when: "try to save a default expired access token"
        token.setExpiration(createDate(-5))
        testClass.storeAccessToken(token, auth)

        authToAccessToken = testClass.getAccessToken(auth)
        authByAccessToken = testClass.readAuthentication(tokenValue)
        accessToken = testClass.readAccessToken(tokenValue)

        then:
        authToAccessToken == null
        authByAccessToken == null
        accessToken == null

        cleanup:
        testClass.removeAccessToken(token.getValue())
    }

    def "check that an refresh token removed correctly"() {
        OAuth2Authentication auth = createAuthentication("test", "test")

        def refreshTokenValue = UUID.randomUUID().toString()
        def refreshToken = new DefaultOAuth2RefreshToken(refreshTokenValue)

        def tokenValue = UUID.randomUUID().toString()
        def token = new DefaultOAuth2AccessToken(tokenValue)
        token.setRefreshToken(refreshToken)

        def keyHelper = new RedisSharedTokenKeyTool()

        when:

        testClass.storeAccessToken(token, auth)
        testClass.storeRefreshToken(refreshToken, auth)

        testClass.removeRefreshToken(refreshToken)
        testClass.removeAccessTokenUsingRefreshToken(refreshToken)

        then:
        testClass.refreshCommand.get(keyHelper.createRefreshTokenKey(refreshToken)) == null
        testClass.refreshCommand.get(keyHelper.createAccessToRefreshKey(refreshTokenValue)) == null

        testClass.refreshTokenCommand.get(keyHelper.createRefreshKey(refreshToken)) == null

        testClass.authenticationCommand.get(keyHelper.createRefreshAuthKey(refreshToken)) == null
        testClass.authenticationCommand.keys(keyHelper.createAuthKey(token)).size() == 1

        testClass.accessTokenCommand.keys(keyHelper.createAccess(token)).size() == 1
        testClass.accessTokenCommand.keys(keyHelper.createAuthToAccessKey(auth)).size() == 1

        when:

        testClass.storeAccessToken(token, auth)
        testClass.storeRefreshToken(refreshToken, auth)

        testClass.removeAccessTokenUsingRefreshToken(refreshToken)
        testClass.removeRefreshToken(refreshToken)

        then:
        assertEmptyKeys(tokenValue, refreshToken, auth)

        cleanup:
        testClass.removeAccessToken(token.getValue())
    }

    void assertEmptyKeys(String token, OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        def keyHelper = new RedisSharedTokenKeyTool()

        testClass.refreshCommand.keys(keyHelper.createRefreshTokenKey(refreshToken)).size() == 0
        testClass.refreshCommand.keys(keyHelper.createAccessToRefreshKey(refreshToken.getValue())).size() == 0

        testClass.refreshTokenCommand.keys(keyHelper.createRefreshKey(refreshToken)).size() == 0

        testClass.authenticationCommand.keys(keyHelper.createRefreshAuthKey(refreshToken)).size() == 0
        testClass.authenticationCommand.keys(keyHelper.createAuthKey(token)).size() == 0

        testClass.accessTokenCommand.keys(keyHelper.createAccess(token)).size() == 0
        testClass.accessTokenCommand.keys(keyHelper.createAuthToAccessKey(authentication)).size() == 0
    }

    def "check that an access token removing correctly"() {
        OAuth2Authentication auth = createAuthentication("test", "test")

        def tokenValue = UUID.randomUUID().toString()
        def token = new DefaultOAuth2AccessToken(tokenValue)

        def refreshTokenValue = UUID.randomUUID().toString()
        def refreshToken = new DefaultOAuth2RefreshToken(refreshTokenValue)

        def keyHelper = new RedisSharedTokenKeyTool()

        when:
        testClass.storeAccessToken(token, auth)

        testClass.removeAccessToken(token)

        then:
        assertEmptyKeys(tokenValue, refreshToken, auth)

        when:
        token.setRefreshToken(refreshToken)
        testClass.storeAccessToken(token, auth)

        testClass.removeAccessToken(token)

        then:
        testClass.refreshCommand.keys(keyHelper.createRefreshTokenKey(refreshToken)).size() == 1

        testClass.refreshTokenCommand.keys(keyHelper.createRefreshKey(refreshToken)).size() == 0

        testClass.authenticationCommand.keys(keyHelper.createRefreshAuthKey(refreshToken)).size() == 0
        testClass.authenticationCommand.keys(keyHelper.createAuthKey(token)).size() == 0

        testClass.accessTokenCommand.keys(keyHelper.createAccess(token)).size() == 0
        testClass.accessTokenCommand.keys(keyHelper.createAuthToAccessKey(auth)).size() == 0

        cleanup:
        testClass.removeRefreshToken(refreshToken)
    }

    def "check that an refresh token is saved"() {
        OAuth2Authentication auth = createAuthentication("test", "test")

        def tokenValue = UUID.randomUUID().toString()
        def expiringTokenValue = UUID.randomUUID().toString()
        def tokenValueWithExpirations = UUID.randomUUID().toString()

        def refreshToken = new DefaultOAuth2RefreshToken(tokenValue)

        def expiringRefreshToken = new DefaultExpiringOAuth2RefreshToken(expiringTokenValue, createDate(-5))

        def refreshTokenWithExpirations = new DefaultExpiringOAuth2RefreshToken(tokenValueWithExpirations, createDate(5))

        when: "try to save a default refresh token"
        testClass.storeRefreshToken(refreshToken, auth)

        def loadedRefreshToken = testClass.readRefreshToken(tokenValue)

        def authByRefreshToken = testClass.readAuthenticationForRefreshToken(refreshToken)

        then:
        loadedRefreshToken != null
        authByRefreshToken != null

        loadedRefreshToken.value == tokenValue
        authByRefreshToken == auth

        when: "try to save a expired refresh token"
        testClass.storeRefreshToken(expiringRefreshToken, auth)

        def loadedExpiringRefreshToken = testClass.readRefreshToken(expiringTokenValue)

        def authByExpiringRefreshToken = testClass.readAuthenticationForRefreshToken(expiringRefreshToken)

        then:
        loadedExpiringRefreshToken == null
        authByExpiringRefreshToken == null

        when: "try to save a refresh token with an expiration property"
        testClass.storeRefreshToken(refreshTokenWithExpirations, auth)

        def loadedActualRefreshToken = testClass.readRefreshToken(tokenValueWithExpirations)

        def authByActualRefreshToken = testClass.readAuthenticationForRefreshToken(refreshTokenWithExpirations)

        then:
        loadedActualRefreshToken != null
        authByActualRefreshToken != null

        loadedActualRefreshToken.value == tokenValueWithExpirations
        authByActualRefreshToken == auth

        cleanup:
        testClass.removeRefreshToken(refreshToken.getValue())
        testClass.removeRefreshToken(expiringRefreshToken.getValue())
        testClass.removeRefreshToken(refreshTokenWithExpirations.getValue())
    }

    def "check that an authentication with different detail stay the token store consistent"() {
        OAuth2Authentication auth = createAuthentication("test", "test")
        OAuth2Authentication auth2 = createAuthentication("test", "test2")

        def tokenValue = UUID.randomUUID().toString()
        def token = new DefaultOAuth2AccessToken(tokenValue)
        testClass.storeAccessToken(token, auth)
        testClass.storeAccessToken(token, auth2)
        def keyHelper = new RedisSharedTokenKeyTool()

        when:
        testClass.getAccessToken(auth)
        testClass.getAccessToken(auth2)

        then:
        testClass.authenticationCommand.get(keyHelper.createAuthKey(tokenValue)) == auth2
    }

    private static Date createDate(int daysToMove) {
        def date = LocalDate.now().plusDays(daysToMove)
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    private OAuth2Authentication createAuthentication(String login, String password) {
        new OAuth2Authentication(new OAuth2Request(), new TestAuthentication(login, password))
    }
}