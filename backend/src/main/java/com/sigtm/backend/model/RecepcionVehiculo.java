package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Recepción e inspección de un vehículo que ingresa al taller.
 *
 * <p>Se persiste en la tabla recepciones_vehiculo y documenta el estado en que llega el vehículo:
 * kilometraje, nivel de combustible, accesorios entregados y estado general. Es la responsable de
 * los daños detectados y de las fotografías de la inspección, y puede enlazarse con la orden de
 * trabajo que se abra a partir de ella.</p>
 */
@Entity
@Table(name = "recepciones_vehiculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionVehiculo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    @JsonIgnoreProperties({"customer", "appointments"})
    private Vehiculo vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_trabajo_id")
    @JsonIgnore
    private OrdenTrabajo workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recibido_por_id")
    @JsonIgnoreProperties({"password", "role", "active", "lastLogin"})
    private Usuario receivedBy;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime receptionDate = LocalDateTime.now();

    @Column(name = "kilometraje")
    private Integer mileage;

    @Column(name = "nivel_combustible", length = 20)
    private String fuelLevel; // Reserva, 1/4, 1/2, 3/4, Lleno

    @Column(name = "estado_exterior", columnDefinition = "TEXT")
    private String exteriorState; // Descripción general del exterior

    @Column(name = "estado_general", columnDefinition = "TEXT")
    private String generalState; // Descripción general del estado

    @Column(name = "accesorios", columnDefinition = "TEXT")
    private String accessories; // JSON/string con accesorios entregados

    @Column(name = "sintomas_cliente", columnDefinition = "TEXT")
    private String customerSymptoms;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "estado", nullable = false, length = 20)
    private String status = "RECIBIDO"; // RECIBIDO, CON_ORDEN, ENTREGADO

    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DanoInspeccion> damages = new ArrayList<>();

    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FotoVehiculo> photos = new ArrayList<>();
}