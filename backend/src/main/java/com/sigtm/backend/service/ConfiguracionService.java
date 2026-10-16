package com.sigtm.backend.service;

import com.sigtm.backend.model.Configuracion;
import com.sigtm.backend.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Administra la configuración única del taller: datos de la empresa, servidor SMTP de
 * correo y preferencias de notificación.
 *
 * <p>Existe un solo registro de configuración: si todavía no hay ninguno, se crea
 * automáticamente con los valores por defecto en la primera lectura.</p>
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private static final String DEFAULT_TALLER_NOMBRE = "Mi Taller Mecánico";

    private final ConfiguracionRepository configuracionRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Obtiene la configuración vigente del taller.
     *
     * <p>Es un acceso con efecto secundario: si no existe ningún registro, se crea y
     * persiste la configuración por defecto antes de devolverla.</p>
     *
     * @return la configuración del sistema
     */
    @Transactional(readOnly = true)
    public Configuracion obtenerConfiguracion() {
        return configuracionRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::crearPorDefecto);
    }

    /**
     * Crea y persiste la configuración inicial del taller.
     *
     * <p>Se usa como recurso de respaldo cuando todavía no existe ningún registro de
     * configuración: taller «{@code Mi Taller Mecánico}», correo {@code GMAIL} con
     * seguridad {@code STARTTLS} en el puerto {@code 587} y todas las notificaciones
     * habilitadas.</p>
     *
     * @return la configuración por defecto ya persistida
     */
    @Transactional
    protected Configuracion crearPorDefecto() {
        Configuracion s = Configuracion.builder()
                .tallerNombre(DEFAULT_TALLER_NOMBRE)
                .correoTipo("GMAIL")
                .smtpSeguridad("STARTTLS")
                .smtpPuerto(587)
                .notifConfirmacionCita(true)
                .notifRecordatorio(true)
                .notifVehiculoListo(true)
                .notifAlertasInventario(true)
                .build();
        return configuracionRepository.save(s);
    }

    /**
     * Actualiza la configuración del taller.
     *
     * <p>Los campos de texto vacíos conservan el valor por defecto para el nombre del
     * taller; el resto de campos solo se sobrescriben cuando llegan informados en la
     * petición. El cambio se registra en la auditoría con la acción {@code UPDATE}.</p>
     *
     * @param actualizar nuevos valores de configuración
     * @return la configuración actualizada
     */
    @Transactional
    public Configuracion actualizar(Configuracion actualizar) {
        Configuracion s = configuracionRepository.findFirstByOrderByIdAsc().orElseGet(this::crearPorDefecto);
        s.setTallerNombre(StringUtils.hasText(actualizar.getTallerNombre()) ? actualizar.getTallerNombre() : DEFAULT_TALLER_NOMBRE);
        s.setTallerLogo(actualizar.getTallerLogo());
        s.setTallerRuc(actualizar.getTallerRuc());
        s.setTallerDireccion(actualizar.getTallerDireccion());
        s.setTallerTelefono(actualizar.getTallerTelefono());
        s.setTallerCorreo(actualizar.getTallerCorreo());
        s.setTallerHorario(actualizar.getTallerHorario());

        if (actualizar.getCorreoTipo() != null) s.setCorreoTipo(actualizar.getCorreoTipo());
        s.setCorreoRemitente(actualizar.getCorreoRemitente());
        s.setSmtpHost(actualizar.getSmtpHost());
        if (actualizar.getSmtpPuerto() != null) s.setSmtpPuerto(actualizar.getSmtpPuerto());
        if (actualizar.getSmtpSeguridad() != null) s.setSmtpSeguridad(actualizar.getSmtpSeguridad());
        if (actualizar.getNotifConfirmacionCita() != null) s.setNotifConfirmacionCita(actualizar.getNotifConfirmacionCita());
        if (actualizar.getNotifRecordatorio() != null) s.setNotifRecordatorio(actualizar.getNotifRecordatorio());
        if (actualizar.getNotifVehiculoListo() != null) s.setNotifVehiculoListo(actualizar.getNotifVehiculoListo());
        if (actualizar.getNotifAlertasInventario() != null) s.setNotifAlertasInventario(actualizar.getNotifAlertasInventario());

        Configuracion guardada = configuracionRepository.save(s);
        auditoriaService.registrar("UPDATE", "CONFIGURACION", null, "Configuración del sistema actualizada");
        return guardada;
    }
}