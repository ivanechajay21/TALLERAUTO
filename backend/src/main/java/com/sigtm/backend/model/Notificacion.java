package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Notificación interna dirigida a un usuario del sistema.
 *
 * <p>Se persiste en la tabla notificaciones y pertenece a un Usuario concreto. Cada aviso indica
 * el módulo del taller que lo originó, un enlace opcional hacia la pantalla correspondiente y
 * si ya fue leído por el destinatario.</p>
 */
@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario user;

    @Column(name = "modulo", nullable = false, length = 50)
    private String module; // INVENTARIO, AGENDA, ORDENES, SISTEMA, RECEPCION

    @Column(name = "titulo", nullable = false, length = 150)
    private String title;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String message;

    @Column(name = "enlace", length = 255)
    private String link;

    @Column(name = "leido", nullable = false)
    private Boolean read = false;
}