package com.sigtm.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Respuesta con el detalle de una recepción de vehículo.
 *
 * <p>Transporta los datos de la recepción (código, estado, fecha, kilometraje,
 * combustible, estado del vehículo, accesorios, síntomas y observaciones),
 * la orden de trabajo asociada, el usuario que realizó la recepción y las
 * listas de daños y fotografías registradas.</p>
 *
 * <p>El vehículo y los daños se representan como mapas dinámicos para permitir
 * exponer distintos campos según el contexto de la consulta.</p>
 */
public record RecepcionResponse(
        Long id,
        String code,
        String status,
        String receptionDate,
        Integer mileage,
        String fuelLevel,
        String exteriorState,
        String generalState,
        List<String> accessories,
        String customerSymptoms,
        String observations,
        String workOrderNumber,
        Map<String, Object> vehicle,
        String receivedByName,
        List<Map<String, Object>> damages,
        List<Map<String, Object>> photos
) {
}