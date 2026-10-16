package com.sigtm.backend.repository;

import com.sigtm.backend.model.RecepcionVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos de las recepciones de vehículos en el taller.
 *
 * <p>Permite consultar el historial de recepciones de un vehículo en orden cronológico, tanto
 * ascendente como descendente, además del listado global de recepciones más recientes.</p>
 */
@Repository
public interface RecepcionVehiculoRepository extends JpaRepository<RecepcionVehiculo, Long> {
    /**
     * Lista las recepciones de un vehículo de la más reciente a la más antigua.
     *
     * @param vehicleId identificador del vehículo.
     * @return recepciones del vehículo ordenadas por fecha de recepción descendente.
     */
    List<RecepcionVehiculo> findByVehicleIdOrderByReceptionDateDesc(Long vehicleId);

    /**
     * Lista las recepciones de un vehículo de la más antigua a la más reciente.
     *
     * @param vehicleId identificador del vehículo.
     * @return recepciones del vehículo ordenadas por fecha de recepción ascendente.
     */
    List<RecepcionVehiculo> findByVehicleIdOrderByReceptionDateAsc(Long vehicleId);

    /**
     * Lista todas las recepciones de la más reciente a la más antigua.
     *
     * @return recepciones ordenadas por fecha de recepción descendente.
     */
    List<RecepcionVehiculo> findAllByOrderByReceptionDateDesc();
}