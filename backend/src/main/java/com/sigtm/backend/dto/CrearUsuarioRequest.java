package com.sigtm.backend.dto;

/**
 * Petición de alta de un usuario del sistema.
 *
 * <p>Transporta los datos de identidad y contacto del nuevo usuario
 * (nombre, apellidos, nombre de usuario, correo y teléfono), el rol que
 * tendrá asignado y la contraseña inicial de acceso.</p>
 */
public record CrearUsuarioRequest(
        String name,
        String lastName,
        String username,
        String email,
        String phone,
        String roleName,
        String password) {
}