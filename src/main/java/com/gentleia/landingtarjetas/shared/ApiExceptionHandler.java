package com.gentleia.landingtarjetas.shared;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("amountPesos", "Importe en pesos"),
            Map.entry("amountUsd", "Importe en USD"),
            Map.entry("cardAlias", "Alias de tarjeta"),
            Map.entry("cardBrand", "Tarjeta"),
            Map.entry("color", "Color"),
            Map.entry("currentInstallment", "Cuota actual"),
            Map.entry("description", "Descripción"),
            Map.entry("minimumPaymentPesos", "Pago mínimo en pesos"),
            Map.entry("name", "Nombre"),
            Map.entry("notes", "Notas"),
            Map.entry("provider", "Proveedor"),
            Map.entry("totalInstallments", "Total de cuotas"),
            Map.entry("totalPesos", "Total en pesos"),
            Map.entry("totalUsd", "Total en USD"),
            Map.entry("type", "Tipo")
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> fieldLabel(error.getField()) + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "La validación de la solicitud falló", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), List.of()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    ResponseEntity<ApiErrorResponse> handleDateParse(DateTimeParseException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Use el formato de mes YYYY-MM", List.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), exception.getReason(), List.of()));
    }

    private String fieldLabel(String field) {
        return FIELD_LABELS.getOrDefault(field, field);
    }
}
