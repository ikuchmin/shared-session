CREATE TABLE SS_SHARED_USER_PERMISSION_STORAGE_ITEM (
    ID         uuid,
    USER_ID    uuid         not null,
    PERMISSION varchar(500) not null,
    --
    primary key (ID)
)^