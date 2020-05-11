package ru.udya.sharedsession.exception;

public class SharedSessionReadingException extends SharedSessionException {
    public SharedSessionReadingException() {
    }

    public SharedSessionReadingException(String message) {
        super(message);
    }

    public SharedSessionReadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedSessionReadingException(Throwable cause) {
        super(cause);
    }
}
