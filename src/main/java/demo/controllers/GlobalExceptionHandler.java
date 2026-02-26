package demo.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import demo.domains.dtos.ErrorDto;
import demo.exceptions.UserNotFoundException;
import jakarta.persistence.OptimisticLockException;
import demo.exceptions.AppointmentNotFoundException;
import demo.exceptions.InvalidAppointmentStatusException;
import demo.exceptions.InvalidAppointmentTypeException;
import demo.exceptions.MinimumBookingTimeException;
import demo.exceptions.OverlapAppointmentException;
import demo.exceptions.RoleNotFoundException;
import demo.exceptions.TimeNotInWorkingHourException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorDto> handleOptimisticLockException(OptimisticLockException ex){
        log.error("Caught OptimisticLockException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("The object is updated somewhere else");
        return new ResponseEntity<>(errorDto, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidAppointmentTypeException.class)
    public ResponseEntity<ErrorDto> handleInvalidAppointmentTypeException(InvalidAppointmentTypeException ex){
        log.error("Caught InvalidAppointmentTypeException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Invalid appointment type");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorDto> handleAppointmentNotFoundException(RoleNotFoundException ex){
        log.error("Caught RoleNotFoundException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Role not found");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidAppointmentStatusException.class)
    public ResponseEntity<ErrorDto> handleAppointmentNotFoundException(InvalidAppointmentStatusException ex){
        log.error("Caught InvalidAppointmentStatusException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Invalid appointment status to be confirmed");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ErrorDto> handleAppointmentNotFoundException(AppointmentNotFoundException ex){
        log.error("Caught AppointmentNotFoundException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Appointment not found");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorDto> handleAuthorizationDeniedException(AuthorizationDeniedException ex){
        log.error("Caught AuthorizationDeniedException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Access Denied");
        return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(OverlapAppointmentException.class)
    public ResponseEntity<ErrorDto> handleOverlapAppointmentException(OverlapAppointmentException ex){
        log.error("Caught OverlapAppointmentException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Overlap appointment");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MinimumBookingTimeException.class)
    public ResponseEntity<ErrorDto> handleMinimumBookingTimeException(MinimumBookingTimeException ex){
        log.error("Caught MinimumBookingTimeException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Appointment does not match minimum time");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TimeNotInWorkingHourException.class)
    public ResponseEntity<ErrorDto> handleInvalidAppointmentTimeException(TimeNotInWorkingHourException ex){
        log.error("Caught InvalidAppointmentTimeException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Invalid appointment end and/or start time");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorDto> handleUserNotFoundException(UserNotFoundException ex){
        log.error("Caught UserNotFoundException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("User not found");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleException(Exception ex){
        log.error("Caught Exception", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("An unknown error occured");
        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
