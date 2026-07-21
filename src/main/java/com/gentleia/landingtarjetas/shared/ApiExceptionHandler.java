package com.gentleia.landingtarjetas.shared;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.gentleia.landingtarjetas.supermarket.SuperItemStockConflictException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String SUPER_PRICE_SOURCES_PATH = "/api/super/price-sources";

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("amountPesos", "Importe en pesos"),
            Map.entry("amountUsd", "Importe en USD"),
            Map.entry("cardAlias", "Alias de tarjeta"),
            Map.entry("cardBrand", "Tarjeta"),
            Map.entry("categoryId", "Categoría"),
            Map.entry("checked", "Marcado"),
            Map.entry("code", "Código de barcode"),
            Map.entry("commercialPresentationLabel", "Presentación comercial"),
            Map.entry("commercialPresentationQuantity", "Cantidad de presentación"),
            Map.entry("commercialPresentationPricePesos", "Precio de presentación"),
            Map.entry("commercialPresentationPriceSourceId", "Fuente del precio"),
            Map.entry("commercialPresentationPriceSourceLabel", "Fuente del precio"),
            Map.entry("commercialPresentationPriceObservedDate", "Fecha observada del precio"),
            Map.entry("color", "Color"),
            Map.entry("currentInstallment", "Cuota actual"),
            Map.entry("currentStock", "Stock actual"),
            Map.entry("description", "Descripción"),
            Map.entry("endMonth", "Mes de fin"),
            Map.entry("format", "Formato de barcode"),
            Map.entry("incomeType", "Tipo de ingreso"),
            Map.entry("habitualObjective", "Objetivo habitual"),
            Map.entry("minimumPaymentPesos", "Pago mínimo en pesos"),
            Map.entry("name", "Nombre"),
            Map.entry("notes", "Notas"),
            Map.entry("observedDate", "Fecha observada de la observación"),
            Map.entry("pricePesos", "Precio observado"),
            Map.entry("priceSourceId", "Fuente de precio"),
            Map.entry("provider", "Proveedor"),
            Map.entry("quantity", "Cantidad"),
            Map.entry("quickQuantity", "Cantidad rápida"),
            Map.entry("recurringMonthly", "Recurrente mensual"),
            Map.entry("sourceLabel", "Fuente de la observación"),
            Map.entry("startMonth", "Mes de inicio"),
            Map.entry("totalInstallments", "Total de cuotas"),
            Map.entry("totalPesos", "Total en pesos"),
            Map.entry("totalUsd", "Total en USD"),
            Map.entry("type", "Tipo"),
            Map.entry("allowNegativeStock", "Permitir stock negativo"),
            Map.entry("unit", "Unidad")
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> fieldLabel(error.getField()) + ": " + error.getDefaultMessage())
                .toList();
        logHandledFailure(request, HttpStatus.BAD_REQUEST, String.join(" | ", details));

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "La validación de la solicitud falló", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        logHandledFailure(request, HttpStatus.BAD_REQUEST, exception.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), List.of()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    ResponseEntity<ApiErrorResponse> handleDateParse(DateTimeParseException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Use el formato de mes YYYY-MM", List.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
        List<String> details = unreadableMessageDetails(exception);
        logHandledFailure(request, HttpStatus.BAD_REQUEST, String.join(" | ", details));
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "El cuerpo de la solicitud no es válido",
                        details
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        logHandledFailure(request, status, exception.getReason());
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), exception.getReason(), List.of()));
    }

    @ExceptionHandler(SuperItemStockConflictException.class)
    ResponseEntity<ApiErrorResponse> handleStockConflict(SuperItemStockConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.stockConflict(
                        HttpStatus.CONFLICT.value(),
                        exception.getMessage(),
                        List.of("Reintente con allowNegativeStock=true para confirmar."),
                        exception.getItemId(),
                        exception.getItemName(),
                        exception.getCurrentStock(),
                        exception.getQuantity(),
                        exception.getResultingStock(),
                        exception.getMovementType().name(),
                        exception.isAllowNegativeStock()
                ));
    }

    private String fieldLabel(String field) {
        return FIELD_LABELS.getOrDefault(field, field);
    }

    private List<String> unreadableMessageDetails(HttpMessageNotReadableException exception) {
        InvalidFormatException invalidFormatException = findInvalidFormatException(exception);
        if (invalidFormatException != null && !invalidFormatException.getPath().isEmpty()) {
            String field = invalidFormatException.getPath()
                    .get(invalidFormatException.getPath().size() - 1)
                    .getFieldName();
            if (LocalDate.class.equals(invalidFormatException.getTargetType())) {
                return List.of(fieldLabel(field) + ": use el formato date-only YYYY-MM-DD");
            }
            return List.of(fieldLabel(field) + ": valor inválido");
        }
        return List.of("Revise que el JSON esté bien formado y que los valores sean válidos");
    }

    private InvalidFormatException findInvalidFormatException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                return invalidFormatException;
            }
            current = current.getCause();
        }
        return null;
    }

    private void logHandledFailure(HttpServletRequest request, HttpStatus status, String reason) {
        if (!isSuperPriceSourcesRequest(request)) {
            return;
        }
        LOGGER.warn("Handled {} failure on {} {}: {}",
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                String.valueOf(reason));
    }

    private boolean isSuperPriceSourcesRequest(HttpServletRequest request) {
        return request != null && request.getRequestURI() != null && request.getRequestURI().startsWith(SUPER_PRICE_SOURCES_PATH);
    }
}
