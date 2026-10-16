package com.sigtm.backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Petición de registro de una recepción de vehículo en el taller.
 *
 * <p>Transporta el estado de entrega del vehículo (kilometraje, combustible,
 * estado exterior y general, accesorios y síntomas del cliente), las
 * observaciones de recepción y el listado de daños detectados durante la
 * inspección inicial, cada uno con su zona, tipo, gravedad y vínculo
 * opcional con una fotografía subida.</p>
 */
@Data
public class CrearRecepcionRequest {

    private Long vehicleId;
    private Integer mileage;
    private String fuelLevel;
    private String exteriorState;
    private String generalState;
    private List<String> accessories = new ArrayList<>();
    private String customerSymptoms;
    private String observations;
    private List<DamageRequest> damages = new ArrayList<>();

    @Data
    public static class DamageRequest {
        private String zone;
        private String zoneLabel;
        private String damageType;
        private String severity;
        private String description;
        /** Observación adicional del daño. */
        private String observation;
        /** Índice dentro del array de fotos subidas para vincular la foto del daño. */
        private Integer photoIndex;
    }
}