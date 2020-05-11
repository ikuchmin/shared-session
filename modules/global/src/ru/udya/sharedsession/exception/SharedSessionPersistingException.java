package ru.udya.sharedsession.exception;

public class SharedSessionPersistingException extends SharedSessionException {
    private static final long serialVersionUID = - 7267326797891008572L;

    public SharedSessionPersistingException() {
    }

    public SharedSessionPersistingException(String message) {
        super(message);
    }

    public SharedSessionPersistingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedSessionPersistingException(Throwable cause) {
        super(cause);
    }
}
