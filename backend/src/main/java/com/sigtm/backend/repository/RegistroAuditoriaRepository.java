package com.sigtm.backend.repository;

import com.sigtm.backend.model.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos del registro de auditoría de la aplicación.
 *
 * <p>La búsqueda combinada filtra por acción y por texto libre, e ignora los criterios que
 * llegan nulos en lugar de descartar el registro.</p>
 */
@Repository
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    /**
     * Lista todos los registros de auditoría del más reciente al más antiguo.
     *
     * @return registros ordenados por fecha y hora descendente.
     */
    List<RegistroAuditoria> findAllByOrderByTimestampDesc();

    /**
     * Busca los registros aplicando filtros opcionales de acción y texto libre.
     *
     * <p>Ignora los criterios que llegan nulos y busca el texto libre de forma parcial sobre
     * el usuario, la entidad y los detalles del registro.</p>
     *
     * @param action acción a filtrar; puede ser {@code null}
     * @param search texto libre; puede ser {@code null}
     * @return los registros que cumplen los filtros, del más reciente al más antiguo
     */
    @Query("SELECT a FROM RegistroAuditoria a WHERE " +
            "(:action IS NULL OR lower(a.action) = lower(:action)) AND " +
            "(:search IS NULL OR lower(a.username) LIKE lower(concat('%', :search, '%')) OR " +
            "lower(a.entity) LIKE lower(concat('%', :search, '%')) OR " +
            "lower(a.details) LIKE lower(concat('%', :search, '%'))) " +
            "ORDER BY a.timestamp DESC")
    List<RegistroAuditoria> search(@Param("action") String action, @Param("search") String search);
}