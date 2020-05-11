package ru.udya.sharedsession.exception;

public class SharedSessionTimeoutException extends SharedSessionException {
    private static final long serialVersionUID = 1679435931658843977L;

    public SharedSessionTimeoutException() {
    }

    public SharedSessionTimeoutException(String message) {
        super(message);
    }

    public SharedSessionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedSessionTimeoutException(Throwable cause) {
        super(cause);
    }
}
