package com.sigtm.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Petición de inicio de sesión.
 *
 * <p>Recibe el nombre de usuario y la contraseña para autenticar al usuario.
 * Ambos campos son obligatorios y se validan antes de intentar la
 * autenticación.</p>
 */
@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
