package ru.udya.sharedsession.exception;

public class SharedSessionOptimisticLockException extends SharedSessionPersistingException {
    public SharedSessionOptimisticLockException() {
    }

    public SharedSessionOptimisticLockException(String message) {
        super(message);
    }

    public SharedSessionOptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedSessionOptimisticLockException(Throwable cause) {
        super(cause);
    }
}
