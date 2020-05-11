package ru.udya.sharedsession.exception;

public class SharedSessionException extends RuntimeException {
    private static final long serialVersionUID = 7512259599177759382L;

    public SharedSessionException() {
    }

    public SharedSessionException(String message) {
        super(message);
    }

    public SharedSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedSessionException(Throwable cause) {
        super(cause);
    }
}
