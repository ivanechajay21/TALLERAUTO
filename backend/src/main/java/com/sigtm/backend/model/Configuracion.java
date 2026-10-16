package com.sigtm.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Parámetros generales de configuración del taller.
 *
 * <p>Se persiste en la tabla configuracion y agrupa los datos del taller, los parámetros del
 * servidor de correo saliente y los interruptores que activan cada tipo de notificación.
 * Es una entidad única: se mantiene un solo registro con los ajustes vigentes del sistema.</p>
 */
@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del taller
    @Column(name = "taller_nombre", length = 150)
    private String tallerNombre;

    @Column(name = "taller_logo", columnDefinition = "MEDIUMTEXT")
    private String tallerLogo;

    @Column(name = "taller_ruc", length = 20)
    private String tallerRuc;

    @Column(name = "taller_direccion", length = 255)
    private String tallerDireccion;

    @Column(name = "taller_telefono", length = 30)
    private String tallerTelefono;

    @Column(name = "taller_correo", length = 120)
    private String tallerCorreo;

    @Column(name = "taller_horario", length = 150)
    private String tallerHorario;

    // Correo saliente
    @Column(name = "correo_tipo", length = 10)
    private String correoTipo; // GMAIL, SMTP

    @Column(name = "correo_remitente", length = 120)
    private String correoRemitente;

    @Column(name = "smtp_host", length = 150)
    private String smtpHost;

    @Column(name = "smtp_puerto")
    private Integer smtpPuerto;

    @Column(name = "smtp_seguridad", length = 10)
    private String smtpSeguridad; // NONE, STARTTLS, SSL

    // Notificaciones
    @Column(name = "notif_confirmacion_cita", nullable = false)
    private Boolean notifConfirmacionCita = true;

    @Column(name = "notif_recordatorio", nullable = false)
    private Boolean notifRecordatorio = true;

    @Column(name = "notif_vehiculo_listo", nullable = false)
    private Boolean notifVehiculoListo = true;

    @Column(name = "notif_alertas_inventario", nullable = false)
    private Boolean notifAlertasInventario = true;
}