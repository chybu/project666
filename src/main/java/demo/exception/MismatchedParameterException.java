package demo.exception;

public class MismatchedParameterException extends RuntimeException{

    public MismatchedParameterException() {
    }

    public MismatchedParameterException(String message) {
        super(message);
    }

    public MismatchedParameterException(Throwable cause) {
        super(cause);
    }

    public MismatchedParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public MismatchedParameterException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
