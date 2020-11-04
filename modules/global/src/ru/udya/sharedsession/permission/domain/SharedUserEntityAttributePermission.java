package ru.udya.sharedsession.permission.domain;

public class SharedUserEntityAttributePermission implements SharedUserPermission {

    protected String entityType;

    protected String entityId;

    protected String entityAttribute;

    protected String entityAttributeValue;

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

    public String getEntityAttribute() {
        return entityAttribute;
    }

    public void setEntityAttribute(String entityAttribute) {
        this.entityAttribute = entityAttribute;
    }

    public String getEntityAttributeValue() {
        return entityAttributeValue;
    }

    public void setEntityAttributeValue(String entityAttributeValue) {
        this.entityAttributeValue = entityAttributeValue;
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

        SharedUserEntityAttributePermission that = (SharedUserEntityAttributePermission) o;

        if (! entityType.equals(that.entityType)) { return false; }
        if (! entityId.equals(that.entityId)) { return false; }
        if (! entityAttribute.equals(that.entityAttribute)) { return false; }
        if (! entityAttributeValue.equals(that.entityAttributeValue)) { return false; }
        return operation.equals(that.operation);
    }

    @Override
    public int hashCode() {
        int result = entityType.hashCode();
        result = 31 * result + entityId.hashCode();
        result = 31 * result + entityAttribute.hashCode();
        result = 31 * result + entityAttributeValue.hashCode();
        result = 31 * result + operation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SharedUserEntityAttributePermission{" +
               "entityType='" + entityType + '\'' +
               ", entityId='" + entityId + '\'' +
               ", entityAttribute='" + entityAttribute + '\'' +
               ", entityAttributeValue='" + entityAttributeValue + '\'' +
               ", operation='" + operation + '\'' +
               '}';
    }
}
