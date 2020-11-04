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

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SharedUserSpecificPermission that = (SharedUserSpecificPermission) o;

        if (! specificPermissionId.equals(that.specificPermissionId)) { return false; }
        return operation.equals(that.operation);
    }

    @Override
    public int hashCode() {
        int result = specificPermissionId.hashCode();
        result = 31 * result + operation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SharedUserSpecificPermission{" +
               "specificPermissionId='" + specificPermissionId + '\'' +
               ", operation='" + operation + '\'' +
               '}';
    }
}
