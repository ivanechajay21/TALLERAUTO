package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de trabajo de reparación o mantenimiento de un vehículo.
 *
 * <p>Se persiste en la tabla ordenes_trabajo y vincula de forma obligatoria a un Cliente y a un
 * Vehiculo, con una cita de origen opcional. Agrupa los ítems de trabajo y repuestos facturados,
 * y su estado evoluciona desde RECIBIDO hasta ENTREGADO.</p>
 */
@Entity
@Table(name = "ordenes_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenTrabajo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_orden", nullable = false, unique = true, length = 20)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"vehicles", "workOrders"})
    private Cliente customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"customer", "workOrders"})
    private Vehiculo vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    @JsonIgnore
    private Cita appointment; // Optional link to appointment

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime receptionDate = LocalDateTime.now();

    @Column(name = "kilometraje_actual")
    private Integer currentMileage;

    @Column(name = "nivel_combustible", length = 20)
    private String fuelLevel; // 1/4, 1/2, 3/4, Full, etc.

    @Column(name = "sintomas_cliente", columnDefinition = "TEXT")
    private String customerSymptoms; // What the customer reports

    @Column(name = "diagnostico_tecnico", columnDefinition = "TEXT")
    private String technicalDiagnosis; // Mechanic's diagnosis

    @Column(name = "prioridad", nullable = false, length = 20)
    private String priority; // Normal, Alta, Urgente

    @Column(name = "estado", nullable = false, length = 30)
    private String status; // RECIBIDO, DIAGNOSTICO, EN_PROCESO, ESPERANDO_REPUESTOS, FINALIZADO, ENTREGADO

    @Column(name = "danos_visuales", columnDefinition = "TEXT")
    private String visualDamages; // E.g., JSON string or text describing scratches/dents

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrdenTrabajo> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.receptionDate == null) {
            this.receptionDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "RECIBIDO";
        }
        if (this.priority == null) {
            this.priority = "NORMAL";
        }
    }
}