package com.sigtm.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Petición de ajuste manual de existencias.
 *
 * <p>Transporta la cantidad a mover, el tipo de movimiento (entrada o salida)
 * y una referencia opcional que identifica el motivo del ajuste (por ejemplo,
 * un número de orden de trabajo o de compra).</p>
 *
 * <p>Los campos incluyen las validaciones de Bean Validation que garantizan
 * que la cantidad sea positiva y que el movimiento sea válido.</p>
 */
public record AjusteStockRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a 0")
        Integer quantity,

        @NotBlank(message = "El tipo de movimiento es obligatorio")
        @Pattern(regexp = "(?i)IN|OUT", message = "El tipo debe ser IN (entrada) u OUT (salida)")
        String type,

        @Size(max = 100, message = "La referencia no puede superar 100 caracteres")
        String reference) {
}