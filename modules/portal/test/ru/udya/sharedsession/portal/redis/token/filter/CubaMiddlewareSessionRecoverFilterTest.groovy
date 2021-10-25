package ru.udya.sharedsession.portal.redis.token.filter

import com.haulmont.addon.restapi.api.config.RestApiConfig
import com.haulmont.cuba.core.global.ClientType
import com.haulmont.cuba.core.global.GlobalConfig
import com.haulmont.cuba.core.global.LocaleResolver
import com.haulmont.cuba.security.app.TrustedClientService
import com.haulmont.cuba.security.auth.AuthenticationService
import com.haulmont.cuba.security.auth.SimpleAuthenticationDetails
import com.haulmont.cuba.security.auth.TrustedClientCredentials
import com.haulmont.cuba.security.entity.User
import com.haulmont.cuba.security.global.LoginException
import com.haulmont.cuba.security.global.UserSession
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.OAuth2Request
import ru.udya.sharedsession.portal.redis.token.TestAuthentication
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class CubaMiddlewareSessionRecoverFilterTest extends Specification {

    private static final String EXISTING_SESSION_ID = UUID.randomUUID().toString()
    private static final String EXISTING_LOGIN = "login"

    CubaMiddlewareSessionRecoverFilter testClass
    Optional<OAuth2Authentication> authenticationOptional = Optional.empty()

    def "check that the next filter will be call"() {
        def doesNextFilterCall = false
        def chain = Stub(FilterChain) {
            doFilter(_, _) >> { ServletRequest request, ServletResponse response ->
                doesNextFilterCall = true
            }
        }

        testClass = new CubaMiddlewareSessionRecoverFilter(Stub(RestApiConfig), Stub(TrustedClientService), Stub(AuthenticationService), Stub(GlobalConfig))

        when:
        testClass.doFilter(null, null, chain)

        then:
        doesNextFilterCall
    }

    def "check that an user session processed by session ID"() {
        def chain = Stub(FilterChain)
        def doesSecurityContextSet = false
        def userSession = null

        def trustedClientService = createTrustedClientServiceStub(createUserSessionStub())

        testClass = new CubaMiddlewareSessionRecoverFilter(Stub(RestApiConfig), trustedClientService, Stub(AuthenticationService), Stub(GlobalConfig)) {
            @Override
            protected Optional<Authentication> getAuthentication() {
                return authenticationOptional
            }

            @Override
            protected void setSecurityContext(UserSession session) {
                userSession = session
                doesSecurityContextSet = true
            }
        }

        when:
        authenticationOptional = createAuthentication(EXISTING_SESSION_ID)
        testClass.doFilter(null, null, chain)

        then:
        noExceptionThrown()
        doesSecurityContextSet
        userSession != null

        when:
        authenticationOptional = createAuthentication(UUID.randomUUID().toString())
        testClass.doFilter(null, null, chain)

        then:
        def e = thrown(RuntimeException)
        e.getMessage() == "Unable to login with trusted client password"
    }

    private UserSession createUserSessionStub() {
        def user = new User()
        user.setLogin("test")
        return new UserSession(UUID.randomUUID(), user, Collections.emptyList(), Locale.forLanguageTag("en"), false)
    }

    def "check that an user session created for a correct login"() {
        def chain = Stub(FilterChain)
        def doesSecurityContextSet = false
        def userSession = null
        def userSessionStub = createUserSessionStub()

        def httpServletRequestStub = Stub(HttpServletRequest)
        def trustedClientService = createTrustedClientServiceStub(userSessionStub)
        def restApiConfig = Stub(RestApiConfig) {
            getTrustedClientPassword() >> "test"
            getSecurityScope() >> null
        }

        def authenticationService = createAuthenticationServiceStub(userSessionStub)
        testClass = new CubaMiddlewareSessionRecoverFilter(restApiConfig, trustedClientService, authenticationService, Stub(GlobalConfig)) {
            @Override
            protected Optional<OAuth2Authentication> getAuthentication() {
                return authenticationOptional
            }

            @Override
            protected void setSecurityContext(UserSession session) {
                userSession = session
                doesSecurityContextSet = true
            }

            @Override
            protected HttpServletRequest getHttpServletRequest() {
                return httpServletRequestStub
            }
        }
        when:
        authenticationOptional = createAuthentication("", "")
        testClass.doFilter(null, null, chain)

        then:
        def e = thrown(IllegalStateException)
        e.getMessage() == "Empty username extracted from user authentication details"

        when:
        authenticationOptional = createAuthentication("", EXISTING_LOGIN)
        testClass.doFilter(null, null, chain)

        then:
        noExceptionThrown()
        userSession != null
        doesSecurityContextSet

        when:
        authenticationOptional = createAuthentication("", "incorrectLogin")
        testClass.doFilter(null, null, chain)

        then:
        e = thrown(OAuth2Exception)
        e.getMessage() == "Cannot login to the middleware"
    }

    private Optional<OAuth2Authentication> createAuthentication(String sessionId) {
        Optional.of(new OAuth2Authentication(new OAuth2Request(), new TestAuthentication(Map.of("sessionId", sessionId))))
    }

    private Optional<OAuth2Authentication> createAuthentication(String sessionId, String username) {
        Optional.of(new OAuth2Authentication(new OAuth2Request(), new TestAuthentication(Map.of("sessionId", sessionId, "username", username))))
    }

    def "check that an user session created with locale"() {
        def chain = Stub(FilterChain)
        def userSession = null
        def doesSecurityContextSet = false
        Locale initialLocale = Locale.forLanguageTag("en")
        Locale processedLocale = null

        def userSessionStub = createUserSessionStub()
        def trustedClientService = createTrustedClientServiceStub(userSessionStub)
        def restApiConfig = createRestConfigStub()

        def authenticationService = createAuthenticationServiceStub(userSessionStub)
        def globalConfig = Stub(GlobalConfig) {
            getAvailableLocales() >> Map.of("English", LocaleResolver.resolve("en"),
                    "Russian", LocaleResolver.resolve("ru"))
        }
        def httpServletRequestStub = Stub(HttpServletRequest) {
            getHeader(_) >> "header"
            getLocale() >> initialLocale
        }
        testClass = new CubaMiddlewareSessionRecoverFilter(restApiConfig, trustedClientService, authenticationService, globalConfig) {
            @Override
            protected Optional<Authentication> getAuthentication() {
                return authenticationOptional
            }

            @Override
            protected void setSecurityContext(UserSession session) {
                userSession = session
                doesSecurityContextSet = true
            }

            @Override
            protected HttpServletRequest getHttpServletRequest() {
                return httpServletRequestStub
            }

            @Override
            protected TrustedClientCredentials createTrustedClientCredentials(String username, Locale locale) {
                processedLocale = locale
                return super.createTrustedClientCredentials(username, locale)
            }
        }

        when:
        authenticationOptional = createAuthentication("", EXISTING_LOGIN)
        testClass.doFilter(null, null, chain)

        then:
        noExceptionThrown()
        userSession != null
        doesSecurityContextSet

        processedLocale == initialLocale
    }

    def "check that a trusted client credentials filled correctly"() {

        Locale initialLocale = Locale.forLanguageTag("en")

        def restApiConfig = Stub(RestApiConfig) {
            getTrustedClientPassword() >> "testPassword"
            getSecurityScope() >> "testScope"
        }
        def httpServletRequestStub = Stub(HttpServletRequest) {
            getHeader(_) >> "header"
            getLocale() >> initialLocale
            getRemoteAddr() >> "testRemoteAddr"
        }
        def globalConfig = Stub(GlobalConfig) {
            getWebHostName() >> "testHostName"
            getWebPort() >> "testPort"
            getWebContextName() >> "testContextName"
        }
        when:
        testClass = new CubaMiddlewareSessionRecoverFilter(restApiConfig, Stub(TrustedClientService), Stub(AuthenticationService), globalConfig) {
            @Override
            protected HttpServletRequest getServletRequestAttributes() {
                return httpServletRequestStub
            }
        }

        def trustedClientCredentials = testClass.createTrustedClientCredentials("testUsername", initialLocale)

        then:
        trustedClientCredentials.login == "testUsername"
        trustedClientCredentials.clientType == ClientType.REST_API
        trustedClientCredentials.securityScope == "testScope"
        trustedClientCredentials.trustedClientPassword == "testPassword"
        trustedClientCredentials.ipAddress == "testRemoteAddr"
        trustedClientCredentials.clientInfo == "REST API (testHostName:testPort/testContextName) header"

        when:
        testClass = new CubaMiddlewareSessionRecoverFilter(restApiConfig, Stub(TrustedClientService), Stub(AuthenticationService), Stub(GlobalConfig)) {
            @Override
            protected HttpServletRequest getServletRequestAttributes() {
                return null
            }
        }

        trustedClientCredentials = testClass.createTrustedClientCredentials("testUsername", initialLocale)

        then:
        trustedClientCredentials.login == "testUsername"
        trustedClientCredentials.clientType == ClientType.REST_API
        trustedClientCredentials.securityScope == "testScope"
        trustedClientCredentials.trustedClientPassword == "testPassword"
        trustedClientCredentials.ipAddress == null
        trustedClientCredentials.clientInfo == "REST API (:/) "
    }

    private RestApiConfig createRestConfigStub() {
        Stub(RestApiConfig) {
            getTrustedClientPassword() >> "test"
            getSecurityScope() >> null
        }
    }

    private AuthenticationService createAuthenticationServiceStub(userSessionStub) {
        Stub(AuthenticationService) {
            login(_) >> { TrustedClientCredentials trustedCred ->
                if (trustedCred.login.equals(EXISTING_LOGIN)) {
                    return new SimpleAuthenticationDetails(userSessionStub)
                }
                throw new LoginException("")
            }
        }
    }

    private TrustedClientService createTrustedClientServiceStub(userSessionStub) {
        Stub(TrustedClientService) {
            findSession(_, _) >> { String trustedClientPassword, UUID sessionId ->
                if (Objects.equals(sessionId, UUID.fromString(EXISTING_SESSION_ID))) {
                    return userSessionStub
                } else
                    throw new LoginException("")
            }
        }
    }
}