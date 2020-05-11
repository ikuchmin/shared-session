package ru.udya.sharedsession.exception;

public class SharedSessionNotFoundException extends SharedSessionException {
    public SharedSessionNotFoundException() {
    }

    public SharedSessionNotFoundException(String message) {
        super(message);
    }

    public SharedSessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedSessionNotFoundException(Throwable cause) {
        super(cause);
    }
}
