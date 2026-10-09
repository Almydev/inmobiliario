package co.inmobiliaria360.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<AuthController.Error> validacion(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthController.Error(msg));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<AuthController.Error> estado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new AuthController.Error(ex.getReason()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<AuthController.Error> parametros(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new AuthController.Error("Parámetro inválido"));
    }
}
