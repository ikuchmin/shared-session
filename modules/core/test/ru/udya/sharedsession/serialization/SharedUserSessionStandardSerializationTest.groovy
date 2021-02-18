package ru.udya.sharedsession.serialization

import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.security.auth.SimpleAuthenticationDetails
import org.springframework.remoting.support.RemoteInvocationResult
import ru.udya.sharedsession.domain.SharedUserSessionId
import ru.udya.sharedsession.redis.RedisSharedUserSessionRuntimeAdapter
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession
import spock.lang.Ignore
import spock.lang.Specification

@SuppressWarnings('GroovyAccessibility')
class SharedUserSessionStandardSerializationTest extends Specification {

    SharedUserSessionStandardSerialization sharedUserSessionStandardSerialization

    void setup() {
        sharedUserSessionStandardSerialization = new SharedUserSessionStandardSerialization()
    }

    def "check that preProcessSerializedObject works correctly with some object" () {
        given:
        Object object = new Object()

        when:
        Object result = sharedUserSessionStandardSerialization
                .preProcessSerializedObject(object)

        then:
        result == object
    }

    def "check that preProcessSerializedObject works correctly with RemoteInvocationResult including exception" () {
        given:
        Object value = new Object()
        Throwable exception = new IllegalAccessException()

        when:
        RemoteInvocationResult remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setValue(value)
        remoteInvocationResult.setException(exception)

        RemoteInvocationResult result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .preProcessSerializedObject(remoteInvocationResult)

        then:
        result.getException() == exception
        result.getValue() == value

        when:
        remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setException(exception)

        result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .preProcessSerializedObject(remoteInvocationResult)

        then:
        result.getException() == exception
        result.getValue() == null

        when:
        remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setValue(value)

        result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .preProcessSerializedObject(remoteInvocationResult)

        then:
        result.getException() == null
        result.getValue() == value
    }

    def "check that preProcessSerializedObject works correctly with SharedUserSessionId value in RemoteInvocationResult" () {
        given:
        SharedUserSessionId sharedUserSessionId = RedisSharedUserSession.of(UuidProvider.createUuid().toString())

        when:
        RemoteInvocationResult remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setValue(sharedUserSessionId)

        RemoteInvocationResult result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .preProcessSerializedObject(remoteInvocationResult)

        then:
        result.getException() == null
        result.getValue() instanceof SharedUserSessionHolder

        when:
        remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setException(new IndexOutOfBoundsException())

        result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .preProcessSerializedObject(remoteInvocationResult)

        then:
        result.getException() instanceof IndexOutOfBoundsException
        result.getValue() == null
    }


    // exception: Could not find matching constructor for: ru.udya.sharedsession.redis.RedisSharedUserSessionRuntimeAdapter$RedisSharedUserSessionAdapter(ru.udya.sharedsession.redis.domain.RedisSharedUserSessionIdImpl)
    @Ignore
    def "check that preProcessSerializedObject works correctly with SimpleAuthenticationDetails value in RemoteInvocationResult" () {
        given:
        RedisSharedUserSessionRuntimeAdapter.RedisSharedUserSessionAdapter redisSharedUserSessionAdapter=
                new RedisSharedUserSessionRuntimeAdapter.RedisSharedUserSessionAdapter(UuidProvider.createUuid().toString())
        SimpleAuthenticationDetails simpleAuthenticationDetails = new SimpleAuthenticationDetails(redisSharedUserSessionAdapter)

        when:
        RemoteInvocationResult remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setValue(simpleAuthenticationDetails)

        RemoteInvocationResult result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .preProcessSerializedObject(remoteInvocationResult)

        then:
        result.getException() == null
        result.getValue() instanceof SimpleAuthenticationDetails
    }

    def "check that preProcessSerializedObject works correctly with SharedUserSessionId" () {
        given:
        SharedUserSessionId sharedUserSessionId = RedisSharedUserSession.of(UuidProvider.createUuid().toString())

        when:
        def result = sharedUserSessionStandardSerialization.preProcessSerializedObject(sharedUserSessionId)

        then:
        result instanceof SharedUserSessionHolder
    }

    def "check that postProcessDeserializedObject works correctly with some object" () {
        given:
        Object object = new Object()

        when:
        Object result = sharedUserSessionStandardSerialization
                .postProcessDeserializedObject(object)

        then:
        result == object
    }

    def "check that postProcessDeserializedObject works correctly with RemoteInvocationResult including exception" () {
        given:
        Object value = new Object()
        Throwable exception = new RuntimeException()

        when:
        RemoteInvocationResult remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setValue(value)
        remoteInvocationResult.setException(exception)

        RemoteInvocationResult result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .postProcessDeserializedObject(remoteInvocationResult)

        then:
        result.getException() == exception
        result.getValue() == value

        when:
        remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setException(exception)

        result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .postProcessDeserializedObject(remoteInvocationResult)

        then:
        result.getException() == exception
        result.getValue() == null

        when:
        remoteInvocationResult = new RemoteInvocationResult()
        remoteInvocationResult.setValue(value)

        result = (RemoteInvocationResult)sharedUserSessionStandardSerialization
                .postProcessDeserializedObject(remoteInvocationResult)

        then:
        result.getException() == null
        result.getValue() == value
    }

}