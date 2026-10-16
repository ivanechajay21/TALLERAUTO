package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Daño detectado durante la inspección de recepción de un vehículo.
 *
 * <p>Se persiste en la tabla danos_inspeccion y pertenece a una RecepcionVehiculo, que es la
 * responsable de sus daños. Cada registro identifica la zona afectada, el tipo de daño y su
 * severidad, y puede enlazar una fotografía del desperfecto.</p>
 */
@Entity
@Table(name = "danos_inspeccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanoInspeccion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepcion_id", nullable = false)
    @JsonIgnore
    private RecepcionVehiculo reception;

    @Column(name = "zona", nullable = false, length = 50)
    private String zone; // Código de la zona (ej. hood, left-front-door)

    @Column(name = "etiqueta_zona", nullable = false, length = 100)
    private String zoneLabel; // Nombre visible de la zona

    @Column(name = "tipo_dano", nullable = false, length = 50)
    private String damageType; // Rayon, Golpe, Abolladura, Rotura, Desgaste, Falta de pieza, Otro

    @Column(name = "severidad", length = 20)
    private String severity; // Leve, Moderado, Severo

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "url_foto", length = 255)
    private String photoUrl;
}