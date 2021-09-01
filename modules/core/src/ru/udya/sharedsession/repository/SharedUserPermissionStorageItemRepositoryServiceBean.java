package ru.udya.sharedsession.repository;

import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.TransactionalDataManager;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;
import ru.udya.querydsl.cuba.core.CubaQueryFactory;
import ru.udya.sharedsession.entity.SharedUserPermissionStorageItem;
import ru.udya.sharedsession.helper.NativeQueryHelper;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service(SharedUserPermissionStorageItemRepositoryService.NAME)
public class SharedUserPermissionStorageItemRepositoryServiceBean
        implements SharedUserPermissionStorageItemRepositoryService {

    protected Persistence persistence;

    protected Metadata metadata;

    protected TransactionalDataManager txDm;

    protected NativeQueryHelper nativeQueryHelper;

    protected CubaQueryFactory cubaQueryFactory;

    public SharedUserPermissionStorageItemRepositoryServiceBean(Persistence persistence,
                                                                Metadata metadata,
                                                                TransactionalDataManager txDm,
                                                                NativeQueryHelper nativeQueryHelper) {
        this.persistence = persistence;
        this.metadata = metadata;
        this.txDm = txDm;
        this.nativeQueryHelper = nativeQueryHelper;
    }

    @PostConstruct
    public void init() {
        cubaQueryFactory = new CubaQueryFactory(txDm, metadata);
    }

    @Override
    public List<SharedUserPermissionStorageItem> findAllByUserId(Id<User, UUID> userId, View view) {

        // do not rewrite on datamanager or querydsl cuba component
        try (var tx = persistence.getTransaction()) {
            var em = persistence.getEntityManager();

            var query = em.createQuery("select p\n" +
                    "  from ss_SharedUserPermissionStorageItem p\n" +
                    " where p.user.id = :user", SharedUserPermissionStorageItem.class)
                    .setParameter("user", userId)
                    .setView(view);

            var loadedItems = query.getResultList();

            tx.commit();

            return loadedItems;
        }
    }

    @Override
    public SharedUserPermissionStorageItem createByUserAndPermission(Id<User, UUID> userId,
                                                                     String permission) {

        var storageItem = metadata.create(SharedUserPermissionStorageItem.class);
        storageItem.setUser(txDm.getReference(userId));
        storageItem.setPermission(permission);

        return txDm.save(storageItem);
    }

    @Override
    public List<SharedUserPermissionStorageItem> createByUserAndPermissions(Id<User, UUID> userId,
                                                                            List<String> permissions) {

        List<SharedUserPermissionStorageItem> storageItems =
                new ArrayList<>(permissions.size());

        for (var permission : permissions) {
            var storageItem = metadata.create(SharedUserPermissionStorageItem.class);
            storageItem.setUser(txDm.getReference(userId));
            storageItem.setPermission(permission);
            storageItems.add(storageItem);
        }

        var storageItemsArray = storageItems
                .toArray(new SharedUserPermissionStorageItem[0]);

        var savedStorageItems = txDm.save(storageItemsArray);

        return savedStorageItems.stream()
                                .map(si -> (SharedUserPermissionStorageItem) si)
                                .collect(Collectors.toList());
    }

    @Override
    public List<SharedUserPermissionStorageItem> createByUsersAndPermission(Ids<User, UUID> userIds,
                                                                            String permission) {

        List<SharedUserPermissionStorageItem> storageItems =
                new ArrayList<>(userIds.size());

        for (var userId : userIds) {
            var storageItem = metadata.create(SharedUserPermissionStorageItem.class);
            storageItem.setUser(txDm.getReference(userId));
            storageItem.setPermission(permission);
            storageItems.add(storageItem);
        }

        var storageItemsArray = storageItems
                .toArray(new SharedUserPermissionStorageItem[0]);

        var savedStorageItems = txDm.save(storageItemsArray);

        return savedStorageItems.stream()
                                .map(si -> (SharedUserPermissionStorageItem) si)
                                .collect(Collectors.toList());
    }

    @Override
    public List<SharedUserPermissionStorageItem> createByUsersAndPermissions(Ids<User, UUID> userIds,
                                                                             List<String> permissions) {

        List<SharedUserPermissionStorageItem> storageItems =
                new ArrayList<>(userIds.size() * permissions.size());

        for (var userId : userIds) {

            for (var permission : permissions) {
                var storageItem = metadata.create(SharedUserPermissionStorageItem.class);
                storageItem.setUser(txDm.getReference(userId));
                storageItem.setPermission(permission);
                storageItems.add(storageItem);
            }
        }

        var storageItemsArray = storageItems
                .toArray(new SharedUserPermissionStorageItem[0]);

        var savedStorageItems = txDm.save(storageItemsArray);

        return savedStorageItems.stream()
                                .map(si -> (SharedUserPermissionStorageItem) si)
                                .collect(Collectors.toList()); }

    @Override
    public void removeAllByUserAndPermission(Id<User, UUID> userId, String permission) {

        try (var tx = persistence.getTransaction()){
            var em = persistence.getEntityManager();

            var query = em.createNativeQuery(
                    "DELETE\n" +
                            "FROM SS_SHARED_USER_PERMISSION_STORAGE_ITEM\n" +
                            "WHERE USER_ID = ?1\n" +
                            "  AND PERMISSION = ?2")
                    .setParameter(1, userId.getValue())
                    .setParameter(2, permission);

            query.executeUpdate();

            tx.commit();
        }
    }

    @Override
    public void removeAllByUserAndPermissions(Id<User, UUID> userId, List<String> permissions) {

        var permissionsParams = nativeQueryHelper
                .generateArrayParameter(permissions.size());

        //language=PostgreSQL
        var deleteQueryTemplate = "DELETE\n" +
                "FROM SS_SHARED_USER_PERMISSION_STORAGE_ITEM\n" +
                "WHERE USER_ID = ?\n" +
                "  AND PERMISSION IN %s";

        var deleteQueryString = String
                .format(deleteQueryTemplate, permissionsParams);

        AtomicInteger paramPosition = new AtomicInteger(1);
        try (var tx = persistence.getTransaction()){
            var em = persistence.getEntityManager();

            var query = em.createNativeQuery(deleteQueryString)
                    .setParameter(paramPosition.getAndIncrement(), userId.getValue());

            for (var permission : permissions) {
                query.setParameter(paramPosition.getAndIncrement(), permission);
            }

            query.executeUpdate();

            tx.commit();
        }

    }

    @Override
    public void removeAllByUsersAndPermission(Ids<User, UUID> userIds, String permission) {

        var userParams = nativeQueryHelper
                .generateArrayParameter(userIds.size());

        //language=PostgreSQL
        var deleteQueryTemplate = "DELETE\n" +
                "FROM SS_SHARED_USER_PERMISSION_STORAGE_ITEM\n" +
                "WHERE PERMISSION = ?\n" +
                "  AND USER_ID IN %s\n";

        var deleteQueryString = String
                .format(deleteQueryTemplate, userParams);

        AtomicInteger paramPosition = new AtomicInteger(1);
        try (var tx = persistence.getTransaction()){
            var em = persistence.getEntityManager();

            var query = em.createNativeQuery(deleteQueryString)
                    .setParameter(paramPosition.getAndIncrement(), permission);

            for (var userId : userIds) {
                query.setParameter(paramPosition.getAndIncrement(), userId.getValue());
            }

            query.executeUpdate();

            tx.commit();
        }
    }

    @Override
    public void removeAllByUsersAndPermissions(Ids<User, UUID> userIds, List<String> permissions) {

        var userParams = nativeQueryHelper
                .generateArrayParameter(userIds.size());

        var permissionsParams = nativeQueryHelper
                .generateArrayParameter(permissions.size());

        //language=PostgreSQL
        var deleteQueryTemplate = "DELETE\n" +
                "FROM SS_SHARED_USER_PERMISSION_STORAGE_ITEM\n" +
                "WHERE USER_ID IN %s\n" +
                "  AND PERMISSION IN %s";

        var deleteQueryString = String
                .format(deleteQueryTemplate, userParams, permissionsParams);


        AtomicInteger paramPosition = new AtomicInteger(1);
        try (var tx = persistence.getTransaction()){
            var em = persistence.getEntityManager();

            var query = em.createNativeQuery(deleteQueryString);

            for (var userId : userIds) {
                query.setParameter(paramPosition.getAndIncrement(), userId.getValue());
            }

            for (var permission : permissions) {
                query.setParameter(paramPosition.getAndIncrement(), permission);
            }

            query.executeUpdate();

            tx.commit();
        }
    }
}
