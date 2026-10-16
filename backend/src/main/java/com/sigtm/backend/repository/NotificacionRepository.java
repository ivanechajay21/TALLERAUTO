package com.sigtm.backend.repository;

import com.sigtm.backend.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos de las notificaciones dirigidas a los usuarios del sistema.
 *
 * <p>Incluye el conteo de notificaciones no leídas, que alimenta el contador del menú lateral.</p>
 */
@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    /**
     * Lista las notificaciones de un usuario de la más reciente a la más antigua.
     *
     * @param userId identificador del usuario.
     * @return notificaciones del usuario ordenadas por fecha de creación descendente.
     */
    List<Notificacion> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Lista las notificaciones pendientes de lectura de un usuario.
     *
     * @param userId identificador del usuario.
     * @return notificaciones no leídas ordenadas por fecha de creación descendente.
     */
    List<Notificacion> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Cuenta las notificaciones pendientes de lectura de un usuario.
     *
     * @param userId identificador del usuario.
     * @return cantidad de notificaciones no leídas.
     */
    long countByUserIdAndReadFalse(Long userId);
}