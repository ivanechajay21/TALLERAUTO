package com.sigtm.backend.service;

import com.sigtm.backend.model.Cita;
import com.sigtm.backend.model.Cliente;
import com.sigtm.backend.model.Vehiculo;
import com.sigtm.backend.repository.CitaRepository;
import com.sigtm.backend.repository.ClienteRepository;
import com.sigtm.backend.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administra las citas de los clientes para sus vehículos, desde el registro inicial
 * hasta los cambios de estado.
 *
 * <p>Las citas nacen siempre en estado {@code PENDING} y toda creación o cambio de
 * estado queda registrado en la auditoría.</p>
 */
@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Lista todas las citas registradas en el sistema.
     *
     * <p>Cada cita se devuelve en formato {@code Map} con los datos del cliente y del
     * vehículo resumidos.</p>
     *
     * @return la lista completa de citas
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerTodas() {
        return citaRepository.findAll().stream().map(this::aMapa).toList();
    }

    /**
     * Obtiene una cita por su identificador.
     *
     * <p>Si la cita no existe se lanza {@code RuntimeException} con el mensaje
     * «Cita no encontrada».</p>
     *
     * @param id identificador de la cita
     * @return la cita solicitada en formato {@code Map}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPorId(Long id) {
        return aMapa(citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada")));
    }

    /**
     * Lista las citas programadas para una fecha concreta.
     *
     * <p>Cada cita se devuelve en formato {@code Map} con los datos del cliente y del
     * vehículo resumidos.</p>
     *
     * @param date fecha de la cita
     * @return las citas de esa fecha
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerPorFecha(LocalDate date) {
        return citaRepository.findByAppointmentDate(date).stream().map(this::aMapa).toList();
    }

    /**
     * Registra una nueva cita para un cliente y su vehículo.
     *
     * <p>La cita nace siempre en estado {@code PENDING}. Si el cliente o el vehículo no
     * existen se lanza {@code RuntimeException}. La creación queda registrada en la
     * auditoría con la acción {@code CREATE}.</p>
     *
     * @param customerId identificador del cliente
     * @param vehicleId  identificador del vehículo
     * @param cita       datos de la cita a registrar
     * @return la cita creada en formato {@code Map}
     */
    @Transactional
    public Map<String, Object> crearCita(Long customerId, Long vehicleId, Cita cita) {
        Cliente customer = clienteRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Vehiculo vehicle = vehiculoRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        cita.setCustomer(customer);
        cita.setVehicle(vehicle);
        cita.setStatus("PENDING");
        Cita guardada = citaRepository.save(cita);
        auditoriaService.registrar("CREATE", "CITA", guardada.getId(),
                "Registro de cita para el vehículo placa " + vehicle.getPlate()
                        + " el " + guardada.getAppointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return aMapa(guardada);
    }

    /**
     * Cambia el estado de una cita.
     *
     * <p>Si la cita no existe se lanza {@code RuntimeException}. El cambio se registra
     * en la auditoría con la acción {@code STATUS} indicando el estado anterior y el
     * nuevo.</p>
     *
     * @param id     identificador de la cita
     * @param status nuevo estado de la cita
     * @return la cita actualizada en formato {@code Map}
     */
    @Transactional
    public Map<String, Object> actualizarEstado(Long id, String status) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        String anterior = cita.getStatus();
        cita.setStatus(status);
        Cita guardada = citaRepository.save(cita);
        auditoriaService.registrar("STATUS", "CITA", guardada.getId(),
                "Cita pasó de " + anterior + " a " + status + " (vehículo " + guardada.getVehicle().getPlate() + ")");
        return aMapa(guardada);
    }

    private Map<String, Object> aMapa(Cita a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("appointmentDate", a.getAppointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        m.put("appointmentTime", a.getAppointmentTime().format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0, 5));
        m.put("requestedService", a.getRequestedService());
        m.put("priority", a.getPriority());
        m.put("status", a.getStatus());
        m.put("observations", a.getObservations());

        if (a.getCustomer() != null) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", a.getCustomer().getId());
            c.put("code", a.getCustomer().getCode());
            c.put("firstName", a.getCustomer().getFirstName());
            c.put("lastName", a.getCustomer().getLastName());
            c.put("documentNumber", a.getCustomer().getDocumentNumber());
            m.put("customer", c);
        }
        if (a.getVehicle() != null) {
            Map<String, Object> v = new HashMap<>();
            v.put("id", a.getVehicle().getId());
            v.put("plate", a.getVehicle().getPlate());
            v.put("brand", a.getVehicle().getBrand());
            v.put("model", a.getVehicle().getModel());
            m.put("vehicle", v);
        }
        return m;
    }
}
