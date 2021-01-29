package ru.udya.sharedsession.permission.helper

import com.haulmont.cuba.core.global.AppBeans
import ru.udya.sharedsession.SharedSessionIntegrationSpecification

class CubaPermissionBuildHelperTest extends SharedSessionIntegrationSpecification {

    private CubaPermissionBuildHelper delegate

    void setup() {
        delegate = AppBeans.get(CubaPermissionBuildHelper)
    }

    def "check that helper has required beans"() {
        expect:
        delegate.stringRepresentationHelper != null
    }

}
