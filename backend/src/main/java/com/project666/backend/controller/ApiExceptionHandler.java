package com.project666.backend.controller;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.OptimisticLockException;
import com.project666.backend.domain.dto.ErrorDto;
import com.project666.backend.exception.InvalidConfirmationTimeWindowException;
import com.project666.backend.exception.InvalidCreateAppointmentTimeWindowException;
import com.project666.backend.exception.OverlapAppointmentException;
import com.project666.backend.exception.TimeNotInWorkingHourException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice(basePackages = "com.project666.backend")
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorDto> handleNoSuchElementException(NoSuchElementException ex){
        log.error("Caught NoSuchElementException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage(ex.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException ex){
        log.error("Caught IllegalArgumentException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage(ex.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("Caught MethodArgumentNotValidException", ex);

        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");

        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage(message);

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidConfirmationTimeWindowException.class)
    public ResponseEntity<ErrorDto> handleOptimisticLockException(InvalidConfirmationTimeWindowException ex){
        log.error("Caught InvalidConfirmationTimeWindowException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Invalid time to confirm an appointment");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorDto> handleOptimisticLockException(OptimisticLockException ex){
        log.error("Caught OptimisticLockException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("The object is updated somewhere else");
        return new ResponseEntity<>(errorDto, HttpStatus.CONFLICT);
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

    @ExceptionHandler(InvalidCreateAppointmentTimeWindowException.class)
    public ResponseEntity<ErrorDto> handleMinimumBookingTimeException(InvalidCreateAppointmentTimeWindowException ex){
        log.error("Caught MinimumBookingTimeException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Appointment is not in booking window (3 days to 31 days ahead)");
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TimeNotInWorkingHourException.class)
    public ResponseEntity<ErrorDto> handleInvalidAppointmentTimeException(TimeNotInWorkingHourException ex){
        log.error("Caught InvalidAppointmentTimeException", ex);
        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorMessage("Invalid appointment end and/or start time");
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
