package com.sigtm.backend.dto;

/**
 * Petición de actualización de una orden de trabajo.
 *
 * <p>Transporta los datos de seguimiento y diagnóstico que se registran
 * durante la ejecución de la reparación: kilometraje y nivel de combustible
 * actuales, síntomas reportados por el cliente, diagnóstico técnico,
 * prioridad y daños visuales detectados en la inspección.</p>
 */
public record ActualizarOrdenTrabajoRequest(
        Integer currentMileage,
        String fuelLevel,
        String customerSymptoms,
        String technicalDiagnosis,
        String priority,
        String visualDamages) {
}