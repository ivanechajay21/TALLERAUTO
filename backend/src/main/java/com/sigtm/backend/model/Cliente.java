package com.sigtm.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Cliente del taller.
 *
 * <p>Se persiste en la tabla clientes y almacena los datos de contacto del cliente junto con
 * el historial de sus vehículos, sus citas y sus órdenes de trabajo.</p>
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", unique = true, length = 20)
    private String code; // Generado automáticamente: CLI-000001

    @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
    private String documentNumber; // DNI/RUC

    @Column(name = "nombres", nullable = false, length = 100)
    private String firstName;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String lastName;

    @Column(name = "telefono", length = 20)
    private String phone;

    @Column(name = "whatsapp", length = 20)
    private String whatsapp;

    @Column(name = "correo", length = 150)
    private String email;

    @Column(name = "direccion", length = 255)
    private String address;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate registrationDate = LocalDate.now();

    @Column(name = "activo", nullable = false)
    private Boolean active = true;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observations;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Vehiculo> vehicles = new ArrayList<>();

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