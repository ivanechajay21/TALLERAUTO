package com.sigtm.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta de una autenticación correcta.
 *
 * <p>Transporta el token JWT emitido y los datos de perfil que el frontend
 * necesita para construir la sesión: identificador, nombre de usuario,
 * correo, roles, nombre completo, imagen de avatar y fecha del último
 * acceso.</p>
 *
 * <p>El campo {@code type} es fijo en {@code Bearer} e indica el esquema de
 * autorización que debe usarse al enviar el token en las peticiones
 * posteriores.</p>
 */
@Getter
@Setter
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private String name;
    private String lastName;
    private String roleName;
    private String avatarUrl;
    private String lastLogin;
    private final String type = "Bearer";
}