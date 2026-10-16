package com.sigtm.backend.repository;

import com.sigtm.backend.model.ItemOrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos de los ítems de una orden de trabajo: servicios, repuestos y mano de obra.
 *
 * <p>Permite obtener el detalle completo de una orden y las tareas asignadas a un mecánico,
 * tanto en general como filtradas por estado.</p>
 */
@Repository
public interface ItemOrdenTrabajoRepository extends JpaRepository<ItemOrdenTrabajo, Long> {
    /**
     * Lista los ítems de una orden de trabajo.
     *
     * @param workOrderId identificador de la orden de trabajo.
     * @return ítems de la orden: servicios, repuestos y mano de obra.
     */
    List<ItemOrdenTrabajo> findByWorkOrderId(Long workOrderId);

    /**
     * Lista las tareas asignadas a un mecánico.
     *
     * @param mechanicId identificador del mecánico.
     * @return ítems asignados al mecánico.
     */
    List<ItemOrdenTrabajo> findByMechanicId(Long mechanicId);

    /**
     * Lista las tareas asignadas a un mecánico que se encuentran en un estado determinado.
     *
     * @param mechanicId identificador del mecánico.
     * @param status     estado del ítem, por ejemplo PENDING, IN_PROGRESS o COMPLETED.
     * @return ítems del mecánico con el estado indicado.
     */
    List<ItemOrdenTrabajo> findByMechanicIdAndStatus(Long mechanicId, String status);
}
