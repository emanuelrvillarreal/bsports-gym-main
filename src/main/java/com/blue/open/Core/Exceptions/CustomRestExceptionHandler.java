package com.blue.open.Core.Exceptions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException ex, WebRequest request) {
        return new ResponseEntity<>(ex, new HttpHeaders(), Objects.requireNonNull(ex.getStatus(), "HttpStatus nulo"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        List<FieldMessage> fields = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fields.add(new FieldMessage(fieldName, errorMessage));
        });

        CustomException customException = new CustomException(
                HttpStatus.BAD_REQUEST, "Error en el formulario", fields.toString());
        customException.setFields(fields);
        return new ResponseEntity<>(customException, new HttpHeaders(),
                Objects.requireNonNull(customException.getStatus(), "HttpStatus nulo"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        CustomException customException = new CustomException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno del Servidor",
                ExceptionUtils.buildDeveloperMessage("CustomRestExceptionHandler.handleAll", ex));
        return new ResponseEntity<>(customException, new HttpHeaders(),
                Objects.requireNonNull(customException.getStatus(), "HttpStatus nulo"));
    }


    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull org.springframework.http.converter.HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        String mensaje = "Error en el cuerpo del request: " + ex.getMostSpecificCause().getMessage();

        CustomException customException = new CustomException(
                HttpStatus.BAD_REQUEST,
                "Error con los datos enviados",
                mensaje
        );

        return new ResponseEntity<>(customException, headers,
                Objects.requireNonNull(customException.getStatus(), "HttpStatus nulo"));
    }
}
