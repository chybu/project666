package demo.exception;

public class OverlapAppointmentException extends RuntimeException{

    public OverlapAppointmentException() {
    }

    public OverlapAppointmentException(String message) {
        super(message);
    }

    public OverlapAppointmentException(Throwable cause) {
        super(cause);
    }

    public OverlapAppointmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public OverlapAppointmentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
