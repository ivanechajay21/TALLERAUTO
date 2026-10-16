package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cita de la agenda para la atención de un vehículo de un cliente.
 *
 * <p>Se persiste en la tabla citas y relaciona de forma obligatoria a un Cliente y a un Vehiculo,
 * con un mecánico asignado de manera opcional. Su estado evoluciona desde PENDING hasta COMPLETED
 * y, al confirmarse, puede dar origen a una OrdenTrabajo.</p>
 */
@Entity
@Table(name = "citas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnore
    private Cliente customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    @JsonIgnore
    private Vehiculo vehicle;

    @Column(name = "fecha_cita", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "hora_cita", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "servicio_solicitado", nullable = false, length = 150)
    private String requestedService;

    @Column(name = "duracion_estimada_minutos")
    private Integer estimatedDurationMinutes;

    @Column(name = "prioridad", length = 20)
    private String priority; // Normal, High, Urgent

    @Column(name = "estado", nullable = false, length = 20)
    private String status; // PENDING, CONFIRMED, CANCELLED, NO_SHOW, IN_PROCESS, COMPLETED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id")
    @JsonIgnore
    private Usuario mechanic;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observations;
}