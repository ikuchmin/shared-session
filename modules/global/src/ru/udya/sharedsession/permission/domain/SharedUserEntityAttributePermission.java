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
}
