package demo.exception;

public class UnknowRoleException extends RuntimeException{

    public UnknowRoleException() {
    }

    public UnknowRoleException(String message) {
        super(message);
    }

    public UnknowRoleException(Throwable cause) {
        super(cause);
    }

    public UnknowRoleException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknowRoleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
