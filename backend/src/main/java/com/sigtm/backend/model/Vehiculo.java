package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Vehículo registrado en el taller.
 *
 * <p>Se persiste en la tabla vehiculos y pertenece a un Cliente, que es su propietario registrado.
 * Un mismo vehículo acumula a lo largo del tiempo sus citas, recepciones y órdenes de trabajo.</p>
 */
@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", unique = true, length = 20)
    private String code; // Generado automáticamente: VEH-000001

    @Column(name = "placa", nullable = false, unique = true, length = 15)
    private String plate;

    @Column(name = "vin", unique = true, length = 50)
    private String vin;

    @Column(name = "marca", nullable = false, length = 50)
    private String brand;

    @Column(name = "modelo", nullable = false, length = 50)
    private String model;

    @Column(name = "anio_vehiculo")
    private Integer year;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "tipo_vehiculo", length = 30)
    private String vehicleType;

    @Column(name = "tipo_combustible", length = 30)
    private String fuelType;

    @Column(name = "transmision", length = 30)
    private String transmission;

    @Column(name = "kilometraje_actual")
    private Integer currentMileage;

    // READ_ONLY: el cliente se resuelve por customerId en el servicio y es ignorado en las peticiones
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("vehicles")
    private Cliente customer;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate registrationDate = LocalDate.now();

    @Column(name = "activo", nullable = false)
    private Boolean active = true;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observations;

    @JsonIgnore
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private List<Cita> appointments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.registrationDate == null) {
            this.registrationDate = LocalDate.now();
        }
        if (this.active == null) {
            this.active = true;
        }
    }
}