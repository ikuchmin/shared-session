package ru.udya.sharedsession.portal.redis.token.codec

import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import ru.udya.sharedsession.portal.redis.token.TestAuthentication
import spock.lang.Specification

class RedisCodecsTest extends Specification {
    RedisOAuth2AuthenticationCodec authCodec
    RedisOAuth2AccessTokenCodec accessTokenCodec
    RedisOAuth2RefreshTokenCodec refreshTokenCodec

    void setup() {
        authCodec = new RedisOAuth2AuthenticationCodec()
        accessTokenCodec = new RedisOAuth2AccessTokenCodec()
        refreshTokenCodec = new RedisOAuth2RefreshTokenCodec()
    }

    def "check that an authentication key codec works correctly"() {
        def key = "qwerty"
        when:
        def byteBuffer = authCodec.encodeKey(key)
        def decodeKey = authCodec.decodeKey(byteBuffer)
        then:
        decodeKey == key
    }

    def "check that an authentication value codec works correctly"() {
        def authentication = new OAuth2Authentication(new OAuth2Request(), new TestAuthentication("login", "password"))
        when:
        def byteBuffer = authCodec.encodeValue(authentication)
        def decodeValue = authCodec.decodeValue(byteBuffer)
        then:
        decodeValue.equals(authentication)
    }

    def "check that an access token key codec works correctly"() {
        def key = "testKey"
        when:
        def byteBuffer = accessTokenCodec.encodeKey(key)
        def decodeKey = accessTokenCodec.decodeKey(byteBuffer)
        then:
        decodeKey == key
    }

    def "check that an access token value codec works correctly"() {
        def tokenValue = UUID.randomUUID().toString()
        def token = new DefaultOAuth2AccessToken(tokenValue)
        when:
        def byteBuffer = accessTokenCodec.encodeValue(token)
        def decodeValue = accessTokenCodec.decodeValue(byteBuffer)
        then:
        decodeValue.equals(token)
    }


    def "check that an refresh token key codec works correctly"() {
        def key = "testKey"
        when:
        def byteBuffer = refreshTokenCodec.encodeKey(key)
        def decodeKey = refreshTokenCodec.decodeKey(byteBuffer)
        then:
        decodeKey == key
    }

    def "check that an refresh token value codec works correctly"() {
        def tokenValue = UUID.randomUUID().toString()
        def expiringRefreshToken = new DefaultExpiringOAuth2RefreshToken(tokenValue, new Date())

        def refreshToken = new DefaultOAuth2RefreshToken(tokenValue)
        when:
        def byteBuffer = refreshTokenCodec.encodeValue(expiringRefreshToken)
        def decodeValue = refreshTokenCodec.decodeValue(byteBuffer)
        then:
        decodeValue.equals(expiringRefreshToken)

        when:
        byteBuffer = refreshTokenCodec.encodeValue(refreshToken)
        decodeValue = refreshTokenCodec.decodeValue(byteBuffer)
        then:
        decodeValue.equals(refreshToken)
    }
}