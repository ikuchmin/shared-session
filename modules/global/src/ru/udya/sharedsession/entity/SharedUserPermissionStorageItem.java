package ru.udya.sharedsession.entity;

import com.haulmont.cuba.core.entity.BaseUuidEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "SS_SHARED_USER_PERMISSION_STORAGE_ITEM")
@Entity(name = "ss_SharedUserPermissionStorageItem")
public class SharedUserPermissionStorageItem extends BaseUuidEntity {
}
