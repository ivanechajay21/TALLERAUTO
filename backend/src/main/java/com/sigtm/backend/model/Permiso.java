package com.sigtm.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Permiso de acceso a una acción concreta de un módulo del sistema.
 *
 * <p>Se persiste en la tabla permisos y describe la tripleta módulo, submódulo y acción que
 * autoriza, junto con su nombre legible. Los permisos se agrupan en el conjunto de permisos
 * concedido a cada Rol.</p>
 */
@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "modulo", nullable = false, length = 60)
    private String module;

    @Column(name = "submodulo", nullable = false, length = 60)
    private String submodule;

    @Column(name = "accion", nullable = false, length = 30)
    private String action;

    @Column(name = "nombre", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "descripcion", length = 255)
    private String description;
}