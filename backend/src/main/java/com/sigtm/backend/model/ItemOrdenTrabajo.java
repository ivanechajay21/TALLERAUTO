package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Concepto o línea de trabajo que forma parte de una orden de trabajo.
 *
 * <p>Se persiste en la tabla items_orden_trabajo y pertenece a una OrdenTrabajo. Cada ítem puede
 * referirse a un ServicioCatalogo, a un Producto del inventario o a un concepto personalizado,
 * y opcionalmente asigna un mecánico responsable con su propio estado y sus fechas de ejecución.</p>
 */
@Entity
@Table(name = "items_orden_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOrdenTrabajo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_trabajo_id", nullable = false)
    @JsonIgnore
    private OrdenTrabajo workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    @JsonIgnore
    private ServicioCatalogo serviceCatalog; // Can be null if it's a custom service/part

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Producto product; // Repuesto de inventario (null si es servicio/mano de obra)

    @Column(name = "descripcion", nullable = false, length = 150)
    private String description;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(name = "cantidad", nullable = false)
    private Integer quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id")
    @JsonIgnore
    private Usuario mechanic; // Assigned mechanic for this specific task

    @Column(name = "estado", nullable = false, length = 30)
    private String status; // PENDING, IN_PROGRESS, COMPLETED

    @Column(name = "fecha_inicio")
    private LocalDateTime startDate;

    @Column(name = "fecha_fin")
    private LocalDateTime endDate;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observations;
}