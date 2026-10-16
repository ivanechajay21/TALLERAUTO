package com.sigtm.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Rol del taller que agrupa los permisos concedidos a sus usuarios.
 *
 * <p>Se persiste en la tabla roles y se relaciona mediante la tabla intermedia roles_permisos
 * con el conjunto de Permiso que puede ejercer. Cada Usuario del sistema tiene un único rol
 * asignado, que determina las acciones a las que tiene acceso.</p>
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "descripcion", length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "roles_permisos",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    private Set<Permiso> permissions = new HashSet<>();
}