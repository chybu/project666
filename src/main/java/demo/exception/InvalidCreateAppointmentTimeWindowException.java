package demo.exception;

public class InvalidCreateAppointmentTimeWindowException extends RuntimeException{

    public InvalidCreateAppointmentTimeWindowException() {
    }

    public InvalidCreateAppointmentTimeWindowException(String message) {
        super(message);
    }

    public InvalidCreateAppointmentTimeWindowException(Throwable cause) {
        super(cause);
    }

    public InvalidCreateAppointmentTimeWindowException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCreateAppointmentTimeWindowException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
