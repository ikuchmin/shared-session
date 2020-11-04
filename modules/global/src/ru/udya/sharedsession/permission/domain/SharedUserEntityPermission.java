package ru.udya.sharedsession.permission.domain;

public class SharedUserEntityPermission implements SharedUserPermission {

    protected String entityType;

    protected String entityId;

    protected String operation;

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
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

        SharedUserEntityPermission that = (SharedUserEntityPermission) o;

        if (! entityType.equals(that.entityType)) { return false; }
        if (! entityId.equals(that.entityId)) { return false; }
        return operation.equals(that.operation);
    }

    @Override
    public int hashCode() {
        int result = entityType.hashCode();
        result = 31 * result + entityId.hashCode();
        result = 31 * result + operation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SharedUserEntityPermission{" +
               "entityType='" + entityType + '\'' +
               ", entityId='" + entityId + '\'' +
               ", operation='" + operation + '\'' +
               '}';
    }
}
