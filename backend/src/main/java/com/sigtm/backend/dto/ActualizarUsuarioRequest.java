package com.sigtm.backend.dto;

/**
 * Petición de actualización de un usuario del sistema.
 *
 * <p>Transporta los datos personales y de acceso modificables de un usuario
 * (nombre, apellidos, correo, teléfono y rol) junto con la nueva contraseña,
 * que solo se aplica cuando se envía informada.</p>
 */
public record ActualizarUsuarioRequest(
        String name,
        String lastName,
        String email,
        String phone,
        String roleName,
        String newPassword) {
}