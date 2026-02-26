package demo.exceptions;

public class InvalidAppointmentTypeException extends RuntimeException{

    public InvalidAppointmentTypeException() {
    }

    public InvalidAppointmentTypeException(String message) {
        super(message);
    }

    public InvalidAppointmentTypeException(Throwable cause) {
        super(cause);
    }

    public InvalidAppointmentTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAppointmentTypeException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
