package com.sigtm.backend.repository;

import com.sigtm.backend.model.ItemCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de los ítems de los catálogos de apoyo del taller.
 *
 * <p>Los catálogos de categoría, estado, prioridad y unidad comparten la misma entidad y se
 * distinguen por el campo tipo; las listas se devuelven en el orden definido para cada catálogo.</p>
 */
@Repository
public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, Long> {
    /**
     * Lista los ítems de un tipo de catálogo en su orden de presentación.
     *
     * @param tipo tipo de catálogo, por ejemplo CATEGORIA, ESTADO, PRIORIDAD o UNIDAD.
     * @return ítems del catálogo ordenados por el campo orden y, a igualdad, por identificador.
     */
    List<ItemCatalogo> findByTipoOrderByOrdenAscIdAsc(String tipo);

    /**
     * Busca un ítem de catálogo por su nombre, ignorando mayúsculas y minúsculas.
     *
     * @param tipo   tipo de catálogo.
     * @param nombre nombre del ítem.
     * @return el ítem encontrado o vacío si no existe.
     */
    Optional<ItemCatalogo> findByTipoAndNombreIgnoreCase(String tipo, String nombre);

    /**
     * Busca un ítem de catálogo por su código dentro de un tipo de catálogo.
     *
     * @param tipo tipo de catálogo.
     * @param code código del ítem.
     * @return el ítem encontrado o vacío si no existe.
     */
    Optional<ItemCatalogo> findByTipoAndCode(String tipo, String code);
}