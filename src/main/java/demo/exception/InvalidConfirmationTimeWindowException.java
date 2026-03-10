package demo.exception;

public class InvalidConfirmationTimeWindowException extends RuntimeException{

    public InvalidConfirmationTimeWindowException() {
    }

    public InvalidConfirmationTimeWindowException(String message) {
        super(message);
    }

    public InvalidConfirmationTimeWindowException(Throwable cause) {
        super(cause);
    }

    public InvalidConfirmationTimeWindowException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConfirmationTimeWindowException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
