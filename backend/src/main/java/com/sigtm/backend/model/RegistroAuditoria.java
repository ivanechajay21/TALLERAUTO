package com.sigtm.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de auditoría de las acciones realizadas sobre los datos del sistema.
 *
 * <p>Se persiste en la tabla auditoria_logs y es de solo lectura para la aplicación: identifica
 * la acción efectuada, la entidad afectada con su identificador, el usuario responsable y la
 * fecha y hora en que ocurrió.</p>
 */
@Entity
@Table(name = "auditoria_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accion", nullable = false, length = 50)
    private String action;

    @Column(name = "entidad", nullable = false, length = 50)
    private String entity;

    @Column(name = "entidad_id")
    private Long entityId;

    @Column(name = "nombre_usuario", nullable = false, length = 50)
    private String username;

    @Column(name = "detalles", columnDefinition = "TEXT")
    private String details;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime timestamp;
}