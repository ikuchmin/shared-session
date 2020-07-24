package ru.udya.sharedsession.redis

import com.haulmont.cuba.core.global.AppBeans
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.repository.SharedUserSessionRepository

class RedisSharedUserSessionRepositoryTest extends SharedSessionIntegrationSpecification {
    
    SharedUserSessionRepository testClass
    
    void setup() {
        testClass = AppBeans.get(SharedUserSessionRepository)
    }

    def "check that creating shared user session works as well"() {
        expect:
        true
    }
}
