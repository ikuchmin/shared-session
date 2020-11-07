package ru.udya.sharedsession.repository

import com.haulmont.cuba.core.entity.contracts.Id
import com.haulmont.cuba.core.entity.contracts.Ids
import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.UuidProvider
import com.haulmont.cuba.core.global.View
import com.haulmont.cuba.core.global.ViewBuilder
import com.haulmont.cuba.security.entity.Group
import com.haulmont.cuba.security.entity.User
import ru.udya.sharedsession.SharedSessionIntegrationSpecification
import ru.udya.sharedsession.entity.SharedUserPermissionStorageItem

class SharedUserPermissionStorageItemRepositoryServiceBeanTest
        extends SharedSessionIntegrationSpecification {

    SharedUserPermissionStorageItemRepositoryServiceBean testClass

    @Override
    void setup() {
        testClass = AppBeans.get(SharedUserPermissionStorageItemRepositoryServiceBean)
    }

    def "check that finding stored permission works as well"() {
        given:
        Group testGroup = metadata.create(Group)
        testGroup.with {
            name = UuidProvider.createUuid()
        }

        def userLogin = UuidProvider.createUuid() as String
        User testUser = metadata.create(User)
        testUser.with {
            login = userLogin
            loginLowerCase = userLogin.toLowerCase()
            group = testGroup
        }

        def predefinedStorageItem = metadata.create(SharedUserPermissionStorageItem)
        predefinedStorageItem.with {
            user = testUser
            permission = '*:*:*'
        }

        dataManager.commit(testGroup, testUser, predefinedStorageItem)

        def view = ViewBuilder.of(SharedUserPermissionStorageItem).add("id").build()

        when:
        def fetchedPermissionStorageItem = testClass.findAllByUserId(Id.of(testUser), view)

        then:
        fetchedPermissionStorageItem == [predefinedStorageItem]
    }

    def "check that removing stored permission for one user works as well"() {
        given:
        Group testGroup = metadata.create(Group)
        testGroup.with {
            name = UuidProvider.createUuid()
        }

        def userLogin = UuidProvider.createUuid() as String
        User testUser = metadata.create(User)
        testUser.with {
            login = userLogin
            loginLowerCase = userLogin.toLowerCase()
            group = testGroup
        }

        def testPermission = '*:*:*'
        def storageItem = metadata.create(SharedUserPermissionStorageItem)
        storageItem.with {
            user = testUser
            permission = testPermission
        }

        def testPermission2 = '*:*:create'
        def storageItem2 = metadata.create(SharedUserPermissionStorageItem)
        storageItem2.with {
            user = testUser
            permission = testPermission2
        }

        dataManager.commit(testGroup, testUser, storageItem, storageItem2)

        def view = ViewBuilder.of(SharedUserPermissionStorageItem).add("id").build()

        when:
        testClass.removeAllByUserAndPermission(Id.of(testUser), testPermission)

        then:
        ! testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem)
        testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem2)

    }

    def "check that removing stored many permissions for one user works as well"() {
        given:
        Group testGroup = metadata.create(Group)
        testGroup.with {
            name = UuidProvider.createUuid()
        }

        def userLogin = UuidProvider.createUuid() as String
        User testUser = metadata.create(User)
        testUser.with {
            login = userLogin
            loginLowerCase = userLogin.toLowerCase()
            group = testGroup
        }

        def testPermission = '*:*:*'
        def storageItem = metadata.create(SharedUserPermissionStorageItem)
        storageItem.with {
            user = testUser
            permission = testPermission
        }
        def testPermission2 = '*:*:create'
        def storageItem2 = metadata.create(SharedUserPermissionStorageItem)
        storageItem2.with {
            user = testUser
            permission = testPermission2
        }

        def testPermission3 = 'sec$User:*:create'
        def storageItem3 = metadata.create(SharedUserPermissionStorageItem)
        storageItem3.with {
            user = testUser
            permission = testPermission3
        }

        dataManager.commit(testGroup, testUser, storageItem, storageItem2, storageItem3)

        def view = ViewBuilder.of(SharedUserPermissionStorageItem).add("id").build()

        when:
        testClass.removeAllByUserAndPermissions(Id.of(testUser), [testPermission,
                                                                  testPermission2])

        then:
        ! testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem)
        ! testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem2)
        testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem3)
    }

    def "check that removing stored permission for many users works as well"() {
        given:
        Group testGroup = metadata.create(Group)
        testGroup.with {
            name = UuidProvider.createUuid()
        }

        def userLogin = UuidProvider.createUuid() as String
        User testUser = metadata.create(User)
        testUser.with {
            login = userLogin
            loginLowerCase = userLogin.toLowerCase()
            group = testGroup
        }

        def userLogin2 = UuidProvider.createUuid() as String
        User testUser2 = metadata.create(User)
        testUser2.with {
            login = userLogin2
            loginLowerCase = userLogin2.toLowerCase()
            group = testGroup
        }

        def testPermission = '*:*:*'
        def storageItem = metadata.create(SharedUserPermissionStorageItem)
        storageItem.with {
            user = testUser
            permission = testPermission
        }

        def testPermission2 = '*:*:create'
        def storageItem2 = metadata.create(SharedUserPermissionStorageItem)
        storageItem2.with {
            user = testUser
            permission = testPermission2
        }

        def storageItem3 = metadata.create(SharedUserPermissionStorageItem)
        storageItem3.with {
            user = testUser2
            permission = testPermission
        }

        def storageItem4 = metadata.create(SharedUserPermissionStorageItem)
        storageItem4.with {
            user = testUser2
            permission = testPermission2
        }

        dataManager.commit(testGroup, testUser, testUser2,
                storageItem, storageItem2, storageItem3, storageItem4)

        def view = ViewBuilder.of(SharedUserPermissionStorageItem).add("id").build()

        when:
        testClass.removeAllByUsersAndPermission(Ids.of([testUser, testUser2]), testPermission)

        then:
        ! testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem)
        testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem2)

        ! testClass.findAllByUserId(Id.of(testUser2), view).contains(storageItem3)
        testClass.findAllByUserId(Id.of(testUser2), view).contains(storageItem4)
    }

    def "check that removing stored many permissions for many users works as well"() {
        given:
        Group testGroup = metadata.create(Group)
        testGroup.with {
            name = UuidProvider.createUuid()
        }

        def userLogin = UuidProvider.createUuid() as String
        User testUser = metadata.create(User)
        testUser.with {
            login = userLogin
            loginLowerCase = userLogin.toLowerCase()
            group = testGroup
        }

        def userLogin2 = UuidProvider.createUuid() as String
        User testUser2 = metadata.create(User)
        testUser2.with {
            login = userLogin2
            loginLowerCase = userLogin2.toLowerCase()
            group = testGroup
        }

        def testPermission = '*:*:*'
        def storageItem = metadata.create(SharedUserPermissionStorageItem)
        storageItem.with {
            user = testUser
            permission = testPermission
        }

        def testPermission2 = '*:*:create'
        def storageItem2 = metadata.create(SharedUserPermissionStorageItem)
        storageItem2.with {
            user = testUser
            permission = testPermission2
        }

        def testPermission3 = 'sec$User:*:create'
        def storageItem3 = metadata.create(SharedUserPermissionStorageItem)
        storageItem3.with {
            user = testUser
            permission = testPermission3
        }

        def storageItem4 = metadata.create(SharedUserPermissionStorageItem)
        storageItem4.with {
            user = testUser2
            permission = testPermission
        }

        def storageItem5 = metadata.create(SharedUserPermissionStorageItem)
        storageItem5.with {
            user = testUser2
            permission = testPermission2
        }

        def testPermission6 = 'sec$User:*:create'
        def storageItem6 = metadata.create(SharedUserPermissionStorageItem)
        storageItem6.with {
            user = testUser2
            permission = testPermission3
        }

        dataManager.commit(testGroup, testUser, testUser2,
                storageItem, storageItem2, storageItem3,
                storageItem4, storageItem5, storageItem6)

        def view = ViewBuilder.of(SharedUserPermissionStorageItem).add("id").build()

        when:
        testClass.removeAllByUsersAndPermissions(Ids.of([testUser, testUser2]), [testPermission, testPermission2])

        then:
        ! testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem)
        ! testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem2)
        testClass.findAllByUserId(Id.of(testUser), view).contains(storageItem3)


        ! testClass.findAllByUserId(Id.of(testUser2), view).contains(storageItem4)
        ! testClass.findAllByUserId(Id.of(testUser2), view).contains(storageItem5)
        testClass.findAllByUserId(Id.of(testUser2), view).contains(storageItem6)
    }
}
