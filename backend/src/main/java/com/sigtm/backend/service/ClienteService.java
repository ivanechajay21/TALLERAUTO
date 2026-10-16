package com.sigtm.backend.service;

import com.sigtm.backend.exception.DuplicateResourceException;
import com.sigtm.backend.exception.ResourceNotFoundException;
import com.sigtm.backend.model.Cliente;
import com.sigtm.backend.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administra el padrón de clientes del taller.
 *
 * <p>El borrado es lógico: los clientes se desactivan ({@code active = false}) para no
 * romper la integridad referencial con vehículos, órdenes de trabajo y facturas.</p>
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final AuditoriaService auditoriaService;

    /**
     * Lista todos los clientes registrados, incluidos los desactivados.
     *
     * @return la colección de clientes almacenada
     */
    @Transactional(readOnly = true)
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }

    /**
     * Obtiene un cliente por su identificador.
     *
     * @param id identificador del cliente
     * @return el cliente solicitado
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    /**
     * Registra un nuevo cliente en el padrón.
     *
     * <p>El documento es obligatorio y único. Si no se informan, se generan el código
     * correlativo {@code CLI-000001}, la fecha de alta y el estado activo. El alta se
     * registra en la auditoría con la acción {@code CREATE}.</p>
     *
     * @param cliente datos del cliente a registrar
     * @return el cliente creado
     * @throws DuplicateResourceException si ya existe un cliente con ese documento
     */
    @Transactional
    public Cliente crearCliente(Cliente cliente) {
        if (clienteRepository.findByDocumentNumber(cliente.getDocumentNumber()).isPresent()) {
            throw new DuplicateResourceException("El cliente con documento " + cliente.getDocumentNumber() + " ya existe.");
        }
        if (cliente.getCode() == null || cliente.getCode().isBlank()) {
            cliente.setCode(codeGeneratorService.nextCode("CLI"));
        }
        if (cliente.getRegistrationDate() == null) {
            cliente.setRegistrationDate(java.time.LocalDate.now());
        }
        if (cliente.getActive() == null) {
            cliente.setActive(true);
        }
        Cliente guardado = clienteRepository.save(cliente);
        auditoriaService.registrar("CREATE", "CLIENTE", guardado.getId(),
                "Registro de cliente " + guardado.getCode() + " — " + guardado.getFirstName() + " " + guardado.getLastName());
        return guardado;
    }

    /**
     * Actualiza los datos de contacto y el estado de un cliente existente.
     *
     * <p>El documento y el código no se modifican. La operación queda registrada en la
     * auditoría con la acción {@code UPDATE}.</p>
     *
     * @param id              identificador del cliente
     * @param detallesCliente nuevos datos del cliente
     * @return el cliente actualizado
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional
    public Cliente actualizarCliente(Long id, Cliente detallesCliente) {
        Cliente cliente = obtenerPorId(id);
        cliente.setFirstName(detallesCliente.getFirstName());
        cliente.setLastName(detallesCliente.getLastName());
        cliente.setPhone(detallesCliente.getPhone());
        cliente.setWhatsapp(detallesCliente.getWhatsapp());
        cliente.setEmail(detallesCliente.getEmail());
        cliente.setAddress(detallesCliente.getAddress());
        cliente.setActive(detallesCliente.getActive());
        cliente.setObservations(detallesCliente.getObservations());
        Cliente guardado = clienteRepository.save(cliente);
        auditoriaService.registrar("UPDATE", "CLIENTE", guardado.getId(),
                "Actualización de cliente " + guardado.getCode() + " — " + guardado.getFirstName() + " " + guardado.getLastName());
        return guardado;
    }

    /**
     * Eliminación lógica: desactiva al cliente (active=false) manteniendo su historial
     * y la integridad referencial con vehículos, órdenes y facturas.
     */
    @Transactional
    public void eliminarCliente(Long id) {
        Cliente cliente = obtenerPorId(id);
        if (Boolean.TRUE.equals(cliente.getActive())) {
            cliente.setActive(false);
            clienteRepository.save(cliente);
            auditoriaService.registrar("DELETE", "CLIENTE", cliente.getId(),
                    "Cliente desactivado: " + cliente.getCode() + " — "
                            + cliente.getFirstName() + " " + cliente.getLastName());
        }
    }
}
