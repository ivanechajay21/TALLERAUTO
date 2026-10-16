package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fotografía tomada durante la recepción de un vehículo en el taller.
 *
 * <p>Se persiste en la tabla fotos_vehiculo y pertenece a una RecepcionVehiculo. Cada imagen
 * se clasifica por categoría (frontal, trasera, lateral, interior, motor, daño u otra) para
 * documentar el estado en que el vehículo ingresa al taller.</p>
 */
@Entity
@Table(name = "fotos_vehiculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FotoVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepcion_id", nullable = false)
    @JsonIgnore
    private RecepcionVehiculo reception;

    @Column(name = "categoria", nullable = false, length = 50)
    private String category; // FRONT, REAR, LEFT, RIGHT, INTERIOR, ENGINE, DAMAGE, OTHER

    @Column(name = "url_foto", nullable = false, length = 255)
    private String photoUrl;

    @Column(name = "subido_en", nullable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}