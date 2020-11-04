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

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SharedUserScreenElementPermission that = (SharedUserScreenElementPermission) o;

        if (! screenId.equals(that.screenId)) { return false; }
        if (! screenElementId.equals(that.screenElementId)) { return false; }
        return operation.equals(that.operation);
    }

    @Override
    public int hashCode() {
        int result = screenId.hashCode();
        result = 31 * result + screenElementId.hashCode();
        result = 31 * result + operation.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SharedUserScreenElementPermission{" +
               "screenId='" + screenId + '\'' +
               ", screenElementId='" + screenElementId + '\'' +
               ", operation='" + operation + '\'' +
               '}';
    }
}
