package com.sigtm.backend.repository;

import com.sigtm.backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a datos de los roles de usuario del sistema.
 *
 * <p>El nombre del rol es la clave con la que se asignan los permisos a los usuarios.</p>
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    /**
     * Busca un rol por su nombre.
     *
     * @param name nombre del rol.
     * @return el rol encontrado o vacío si el nombre no existe.
     */
    Optional<Rol> findByName(String name);
}
