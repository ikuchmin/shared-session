package ru.udya.sharedsession

import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.DataManager
import com.haulmont.cuba.core.global.EntityStates
import com.haulmont.cuba.core.global.Metadata
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class SharedSessionIntegrationSpecification extends Specification {

    @ClassRule
    @Shared
    SharedsessionTestContainer container =
            SharedsessionTestContainer.Common.INSTANCE

    Metadata metadata
    DataManager dataManager
    EntityStates entityStates

    void setup() {
        metadata = AppBeans.get(Metadata)
        dataManager = AppBeans.get(DataManager)
        entityStates = AppBeans.get(EntityStates)
    }
}