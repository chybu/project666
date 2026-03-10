package demo.exception;

public class InvalidAppointmentStatusException extends RuntimeException{

    public InvalidAppointmentStatusException() {
    }

    public InvalidAppointmentStatusException(String message) {
        super(message);
    }

    public InvalidAppointmentStatusException(Throwable cause) {
        super(cause);
    }

    public InvalidAppointmentStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAppointmentStatusException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
