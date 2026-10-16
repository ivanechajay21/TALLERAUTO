package com.sigtm.backend.repository;

import com.sigtm.backend.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a datos de la configuración general del taller.
 *
 * <p>El sistema trabaja con una única fila de configuración, por lo que las consultas devuelven
 * siempre el registro de menor identificador.</p>
 */
@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {
    /**
     * Obtiene el registro de configuración vigente.
     *
     * @return la configuración almacenada o vacía si todavía no se ha definido ninguna.
     */
    Optional<Configuracion> findFirstByOrderByIdAsc();
}