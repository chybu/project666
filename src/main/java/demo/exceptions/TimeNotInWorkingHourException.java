package demo.exceptions;

public class TimeNotInWorkingHourException extends RuntimeException{

    public TimeNotInWorkingHourException() {
    }

    public TimeNotInWorkingHourException(String message) {
        super(message);
    }

    public TimeNotInWorkingHourException(Throwable cause) {
        super(cause);
    }

    public TimeNotInWorkingHourException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimeNotInWorkingHourException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
