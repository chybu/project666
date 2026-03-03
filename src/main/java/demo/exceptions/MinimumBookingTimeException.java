package demo.exceptions;

public class MinimumBookingTimeException extends RuntimeException{

    public MinimumBookingTimeException() {
    }

    public MinimumBookingTimeException(String message) {
        super(message);
    }

    public MinimumBookingTimeException(Throwable cause) {
        super(cause);
    }

    public MinimumBookingTimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MinimumBookingTimeException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
