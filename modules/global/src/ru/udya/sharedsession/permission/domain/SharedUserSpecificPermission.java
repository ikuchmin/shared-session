package ru.udya.sharedsession.permission.domain;

public class SharedUserSpecificPermission implements SharedUserPermission{

    protected String specificPermissionId;

    protected String operation;

    public String getSpecificPermissionId() {
        return specificPermissionId;
    }

    public void setSpecificPermissionId(String specificPermissionId) {
        this.specificPermissionId = specificPermissionId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
