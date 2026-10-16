package com.sigtm.backend.service;

import com.sigtm.backend.dto.CrearRecepcionRequest;
import com.sigtm.backend.dto.RecepcionResponse;
import com.sigtm.backend.model.DanoInspeccion;
import com.sigtm.backend.model.FotoVehiculo;
import com.sigtm.backend.model.OrdenTrabajo;
import com.sigtm.backend.model.RecepcionVehiculo;
import com.sigtm.backend.model.Usuario;
import com.sigtm.backend.model.Vehiculo;
import com.sigtm.backend.repository.CitaRepository;
import com.sigtm.backend.repository.OrdenTrabajoRepository;
import com.sigtm.backend.repository.RecepcionVehiculoRepository;
import com.sigtm.backend.repository.UsuarioRepository;
import com.sigtm.backend.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registra la recepción de vehículos en el taller, con inspección, fotografías y daños
 * detectados, y permite emitir la orden de trabajo derivada.
 *
 * <p>Cada recepción recibe un código correlativo {@code REC-000001}; al emitir la orden
 * pasa a estado {@code CON_ORDEN} y ya no puede volver a generar otra.</p>
 */
@Service
public class RecepcionService {

    private final RecepcionVehiculoRepository recepcionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final CitaRepository citaRepository;
    private final StorageService storageService;
    private final CodeGeneratorService codeGeneratorService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public RecepcionService(RecepcionVehiculoRepository recepcionRepository,
                            VehiculoRepository vehiculoRepository,
                            UsuarioRepository usuarioRepository,
                            OrdenTrabajoRepository ordenTrabajoRepository,
                            CitaRepository citaRepository,
                            StorageService storageService,
                            CodeGeneratorService codeGeneratorService,
                            NotificacionService notificacionService,
                            AuditoriaService auditoriaService) {
        this.recepcionRepository = recepcionRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.ordenTrabajoRepository = ordenTrabajoRepository;
        this.citaRepository = citaRepository;
        this.storageService = storageService;
        this.codeGeneratorService = codeGeneratorService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Registra la recepción de un vehículo con su inspección, fotografías y daños.
     *
     * <p>Las fotografías se almacenan en disco y se asocian por índice a cada daño
     * reportado; además se actualiza el odómetro del vehículo si el kilometraje declarado
     * es mayor al registrado, se notifica a gerencia y administración y se deja traza en
     * la auditoría. Cualquier error de E/S al guardar las imágenes se reenvía como
     * {@code RuntimeException} y revierte la transacción completa.</p>
     *
     * @param solicitud    datos de la recepción y lista de daños detectados
     * @param fotos         fotografías del vehículo; puede ser {@code null}
     * @param idRecibidoPor identificador del usuario que recibe el vehículo; puede ser {@code null}
     * @return la recepción persistida con sus daños y fotografías
     */
    @Transactional
    public RecepcionVehiculo crearRecepcion(CrearRecepcionRequest solicitud, List<MultipartFile> fotos, Long idRecibidoPor) {
        try {
            return crearRecepcionInterna(solicitud, fotos, idRecibidoPor);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar las fotografías de la recepción: " + e.getMessage());
        }
    }

    private RecepcionVehiculo crearRecepcionInterna(CrearRecepcionRequest solicitud, List<MultipartFile> fotos, Long idRecibidoPor) throws IOException {
        Vehiculo vehiculo = vehiculoRepository.findById(solicitud.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
        Usuario recibidoPor = idRecibidoPor != null
                ? usuarioRepository.findById(idRecibidoPor).orElse(null)
                : null;

        String[] categorias = {"FRONT", "REAR", "LEFT", "RIGHT", "INTERIOR", "ENGINE", "OTHER"};
        List<String> urlsFotos = new ArrayList<>();
        if (fotos != null) {
            for (MultipartFile foto : fotos) {
                String url = storageService.store(foto);
                if (url != null) {
                    urlsFotos.add(url);
                }
            }
        }

        RecepcionVehiculo recepcion = RecepcionVehiculo.builder()
                .code(codeGeneratorService.nextCode("REC"))
                .vehicle(vehiculo)
                .receivedBy(recibidoPor)
                .receptionDate(LocalDateTime.now())
                .mileage(solicitud.getMileage())
                .fuelLevel(solicitud.getFuelLevel())
                .exteriorState(solicitud.getExteriorState())
                .generalState(solicitud.getGeneralState())
                .accessories(String.join(", ", solicitud.getAccessories()))
                .customerSymptoms(solicitud.getCustomerSymptoms())
                .observations(solicitud.getObservations())
                .status("RECIBIDO")
                .build();

        List<DanoInspeccion> danos = new ArrayList<>();
        for (CrearRecepcionRequest.DamageRequest d : solicitud.getDamages()) {
            String photoUrl = d.getPhotoIndex() != null && d.getPhotoIndex() < urlsFotos.size()
                    ? urlsFotos.get(d.getPhotoIndex())
                    : null;
            danos.add(DanoInspeccion.builder()
                    .reception(recepcion)
                    .zone(d.getZone())
                    .zoneLabel(d.getZoneLabel())
                    .damageType(d.getDamageType())
                    .severity(d.getSeverity())
                    .description(d.getDescription())
                    .observation(d.getObservation())
                    .photoUrl(photoUrl)
                    .build());
        }
        recepcion.setDamages(danos);

        List<FotoVehiculo> fotosVehiculo = new ArrayList<>();
        for (int i = 0; i < urlsFotos.size(); i++) {
            String category = i < categorias.length ? categorias[i] : "OTHER";
            fotosVehiculo.add(FotoVehiculo.builder()
                    .reception(recepcion)
                    .category(category)
                    .photoUrl(urlsFotos.get(i))
                    .build());
        }
        recepcion.setPhotos(fotosVehiculo);

        if (solicitud.getMileage() != null && (vehiculo.getCurrentMileage() == null || solicitud.getMileage() > vehiculo.getCurrentMileage())) {
            vehiculo.setCurrentMileage(solicitud.getMileage());
            vehiculoRepository.save(vehiculo);
        }

        RecepcionVehiculo guardada = recepcionRepository.save(recepcion);
        notificacionService.notificarRol(List.of("ROLE_MANAGER", "ROLE_ADMIN"), "RECEPCION",
                "Nueva recepción de vehículo",
                "Se registró la recepción " + guardada.getCode() + " para el vehículo " + vehiculo.getPlate(),
                "/recepcion");
        auditoriaService.registrar("CREATE", "RECEPCION", guardada.getId(),
                "Registro de recepción " + guardada.getCode() + " para el vehículo " + vehiculo.getPlate()
                        + (recibidoPor != null ? " por " + recibidoPor.getUsername() : ""));
        return guardada;
    }

    /**
     * Emite la orden de trabajo derivada de una recepción.
     *
     * <p>Traslada a la orden los datos del vehículo, el kilometraje, el combustible y los
     * síntomas declarados en recepción, asigna prioridad {@code NORMAL} y estado
     * {@code RECIBIDO}, y marca la recepción como {@code CON_ORDEN}. Solo puede emitirse
     * una orden por recepción. Se notifica a los roles de órdenes y se registra la
     * auditoría.</p>
     *
     * @param receptionId identificador de la recepción de origen
     * @return la orden de trabajo creada
     */
    @Transactional
    public OrdenTrabajo emitirOrdenTrabajo(Long receptionId) {
        RecepcionVehiculo recepcion = recepcionRepository.findById(receptionId)
                .orElseThrow(() -> new RuntimeException("Recepción no encontrada"));
        if (recepcion.getWorkOrder() != null) {
            throw new RuntimeException("Esta recepción ya tiene una orden de trabajo emitida");
        }
        OrdenTrabajo workOrder = new OrdenTrabajo();
        workOrder.setOrderNumber(codeGeneratorService.nextCode("OT"));
        workOrder.setCustomer(recepcion.getVehicle().getCustomer());
        workOrder.setVehicle(recepcion.getVehicle());
        workOrder.setReceptionDate(recepcion.getReceptionDate());
        workOrder.setCurrentMileage(recepcion.getMileage());
        workOrder.setFuelLevel(recepcion.getFuelLevel());
        workOrder.setCustomerSymptoms(recepcion.getCustomerSymptoms());
        workOrder.setPriority("NORMAL");
        workOrder.setStatus("RECIBIDO");
        workOrder.setVisualDamages(recepcion.getDamages().size() + " daños registrados en la inspección");

        OrdenTrabajo guardada = ordenTrabajoRepository.save(workOrder);
        recepcion.setWorkOrder(guardada);
        recepcion.setStatus("CON_ORDEN");
        recepcionRepository.save(recepcion);

        notificacionService.notificarRol(List.of("ROLE_MANAGER", "ROLE_ADMIN", "ROLE_RECEPTIONIST"), "ORDENES",
                "Nueva orden de trabajo",
                "Se emitió la orden " + guardada.getOrderNumber() + " para el vehículo " + recepcion.getVehicle().getPlate(),
                "/ordenes-trabajo");
        auditoriaService.registrar("CREATE", "ORDEN_TRABAJO", guardada.getId(),
                "Emisión de la orden " + guardada.getOrderNumber() + " para el vehículo " + recepcion.getVehicle().getPlate());
        return guardada;
    }

    /**
     * Lista todas las recepciones, de la más reciente a la más antigua.
     *
     * @return las recepciones en formato {@link RecepcionResponse}
     */
    @Transactional(readOnly = true)
    public List<RecepcionResponse> obtenerTodas() {
        return recepcionRepository.findAllByOrderByReceptionDateDesc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    /**
     * Lista las recepciones de un vehículo, de la más reciente a la más antigua.
     *
     * @param vehicleId identificador del vehículo
     * @return las recepciones del vehículo en formato {@link RecepcionResponse}
     */
    @Transactional(readOnly = true)
    public List<RecepcionResponse> obtenerPorVehiculo(Long vehicleId) {
        return recepcionRepository.findByVehicleIdOrderByReceptionDateDesc(vehicleId).stream()
                .map(this::aRespuesta)
                .toList();
    }

    /**
     * Obtiene una recepción por su identificador.
     *
     * <p>Si la recepción no existe se lanza {@code RuntimeException} con el mensaje
     * «Recepción no encontrada».</p>
     *
     * @param id identificador de la recepción
     * @return la recepción solicitada en formato {@link RecepcionResponse}
     */
    @Transactional(readOnly = true)
    public RecepcionResponse obtenerPorId(Long id) {
        RecepcionVehiculo recepcion = recepcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recepción no encontrada"));
        return aRespuesta(recepcion);
    }

    private RecepcionResponse aRespuesta(RecepcionVehiculo r) {
        Map<String, Object> datosVehiculo = new HashMap<>();
        datosVehiculo.put("id", r.getVehicle().getId());
        datosVehiculo.put("code", r.getVehicle().getCode());
        datosVehiculo.put("plate", r.getVehicle().getPlate());
        datosVehiculo.put("brand", r.getVehicle().getBrand());
        datosVehiculo.put("model", r.getVehicle().getModel());
        datosVehiculo.put("year", r.getVehicle().getYear());
        datosVehiculo.put("color", r.getVehicle().getColor());
        if (r.getVehicle().getCustomer() != null) {
            Map<String, Object> datosCliente = new HashMap<>();
            datosCliente.put("id", r.getVehicle().getCustomer().getId());
            datosCliente.put("code", r.getVehicle().getCustomer().getCode());
            datosCliente.put("firstName", r.getVehicle().getCustomer().getFirstName());
            datosCliente.put("lastName", r.getVehicle().getCustomer().getLastName());
            datosCliente.put("documentNumber", r.getVehicle().getCustomer().getDocumentNumber());
            datosCliente.put("phone", r.getVehicle().getCustomer().getPhone());
            datosVehiculo.put("customer", datosCliente);
        }

        List<Map<String, Object>> listaDanos = r.getDamages().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("zone", d.getZone());
            m.put("zoneLabel", d.getZoneLabel());
            m.put("damageType", d.getDamageType());
            m.put("severity", d.getSeverity());
            m.put("description", d.getDescription());
            m.put("observation", d.getObservation());
            m.put("photoUrl", d.getPhotoUrl());
            return m;
        }).toList();

        List<Map<String, Object>> listaFotos = r.getPhotos().stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("category", p.getCategory());
            m.put("photoUrl", p.getPhotoUrl());
            return m;
        }).toList();

        return new RecepcionResponse(
                r.getId(),
                r.getCode(),
                r.getStatus(),
                r.getReceptionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                r.getMileage(),
                r.getFuelLevel(),
                r.getExteriorState(),
                r.getGeneralState(),
                (r.getAccessories() == null || r.getAccessories().isBlank())
                        ? List.of()
                        : Arrays.stream(r.getAccessories().split(",\\s*")).filter(s -> !s.isBlank()).toList(),
                r.getCustomerSymptoms(),
                r.getObservations(),
                r.getWorkOrder() != null ? r.getWorkOrder().getOrderNumber() : null,
                datosVehiculo,
                r.getReceivedBy() != null ? r.getReceivedBy().getName() + " " + r.getReceivedBy().getLastName() : null,
                listaDanos,
                listaFotos);
    }
}