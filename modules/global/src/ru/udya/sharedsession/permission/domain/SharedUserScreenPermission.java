package ru.udya.sharedsession.permission.domain;

public class SharedUserScreenPermission implements SharedUserPermission {

    protected String screenId;

    protected String operation;

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
