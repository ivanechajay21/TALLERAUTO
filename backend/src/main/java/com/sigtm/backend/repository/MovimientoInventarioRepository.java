package com.sigtm.backend.repository;

import com.sigtm.backend.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos de los movimientos de entrada y salida del inventario.
 *
 * <p>Permite reconstruir el historial de un producto o recuperar los movimientos asociados a una
 * referencia de operación, como una orden de trabajo o una orden de compra.</p>
 */
@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    /**
     * Lista los movimientos de un producto.
     *
     * @param productId identificador del producto.
     * @return movimientos de entrada y salida asociados al producto.
     */
    List<MovimientoInventario> findByProductId(Long productId);

    /**
     * Lista los movimientos de un producto del más reciente al más antiguo.
     *
     * @param productId identificador del producto.
     * @return movimientos del producto ordenados por fecha de transacción descendente.
     */
    List<MovimientoInventario> findByProductIdOrderByTransactionDateDesc(Long productId);

    /**
     * Busca los movimientos generados por una referencia de operación.
     *
     * @param reference referencia que originó el movimiento, por ejemplo una orden de trabajo
     *                  o una orden de compra.
     * @return movimientos asociados a la referencia indicada.
     */
    List<MovimientoInventario> findByReference(String reference);
}