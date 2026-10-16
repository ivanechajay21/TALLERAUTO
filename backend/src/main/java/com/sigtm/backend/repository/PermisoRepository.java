package com.sigtm.backend.repository;

import com.sigtm.backend.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de los permisos de acceso al sistema.
 *
 * <p>El listado agrupado por módulo, sub módulo y acción reproduce el orden en que los permisos
 * se presentan en la pantalla de configuración de roles.</p>
 */
@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    /**
     * Busca un permiso por su código.
     *
     * @param code código del permiso.
     * @return el permiso encontrado o vacío si el código no existe.
     */
    Optional<Permiso> findByCode(String code);

    /**
     * Lista todos los permisos en el orden en que se muestran en la configuración de roles.
     *
     * @return permisos ordenados por módulo, sub módulo y acción, de forma ascendente.
     */
    List<Permiso> findAllByOrderByModuleAscSubmoduleAscActionAsc();
}