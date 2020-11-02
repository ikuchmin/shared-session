package ru.udya.sharedsession.permission.domain;

public class SharedUserScreenElementPermission implements SharedUserPermission {

    protected String screenId;

    protected String screenElementId;

    protected String operation;

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public String getScreenElementId() {
        return screenElementId;
    }

    public void setScreenElementId(String screenElementId) {
        this.screenElementId = screenElementId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
