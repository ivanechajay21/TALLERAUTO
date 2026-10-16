package com.sigtm.backend.service;

import com.sigtm.backend.model.ServicioCatalogo;
import com.sigtm.backend.repository.ServicioCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administra el catálogo de servicios ofrecidos por el taller, con sus precios por
 * defecto y tiempos estimados de ejecución.
 */
@Service
@RequiredArgsConstructor
public class ServicioCatalogoService {

    private final ServicioCatalogoRepository servicioCatalogoRepository;

    /**
     * Lista los servicios del catálogo que están activos.
     *
     * @return los servicios activos para ofrecer en las órdenes de trabajo
     */
    @Transactional(readOnly = true)
    public List<ServicioCatalogo> obtenerActivos() {
        return servicioCatalogoRepository.findByActiveTrue();
    }

    /**
     * Obtiene un servicio del catálogo por su identificador.
     *
     * <p>Si el servicio no existe se lanza {@code RuntimeException} con el mensaje
     * «Servicio no encontrado en el catálogo».</p>
     *
     * @param id identificador del servicio
     * @return el servicio solicitado
     */
    @Transactional(readOnly = true)
    public ServicioCatalogo obtenerPorId(Long id) {
        return servicioCatalogoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado en el catálogo"));
    }

    /**
     * Registra un nuevo servicio en el catálogo.
     *
     * <p>El código es obligatorio y debe ser único; si ya existe se lanza
     * {@code RuntimeException}.</p>
     *
     * @param servicio datos del servicio a registrar
     * @return el servicio creado
     */
    @Transactional
    public ServicioCatalogo crearServicio(ServicioCatalogo servicio) {
        if (servicioCatalogoRepository.findByCode(servicio.getCode()).isPresent()) {
            throw new RuntimeException("Ya existe un servicio con ese código");
        }
        return servicioCatalogoRepository.save(servicio);
    }

    /**
     * Actualiza la ficha de un servicio del catálogo.
     *
     * <p>Todos los campos del servicio se sobrescriben con los valores recibidos. Si el
     * servicio no existe se lanza {@code RuntimeException}.</p>
     *
     * @param id               identificador del servicio
     * @param detallesServicio nuevos datos del servicio
     * @return el servicio actualizado
     */
    @Transactional
    public ServicioCatalogo actualizarServicio(Long id, ServicioCatalogo detallesServicio) {
        ServicioCatalogo servicio = obtenerPorId(id);
        servicio.setName(detallesServicio.getName());
        servicio.setDescription(detallesServicio.getDescription());
        servicio.setCategory(detallesServicio.getCategory());
        servicio.setDefaultPrice(detallesServicio.getDefaultPrice());
        servicio.setEstimatedDurationMinutes(detallesServicio.getEstimatedDurationMinutes());
        servicio.setActive(detallesServicio.getActive());
        return servicioCatalogoRepository.save(servicio);
    }
}
