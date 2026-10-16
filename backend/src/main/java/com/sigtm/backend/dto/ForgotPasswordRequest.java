package com.sigtm.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Petición de recuperación de contraseña.
 *
 * <p>Transporta el correo electrónico de la cuenta desde la que se solicita
 * el envío del enlace de restablecimiento. El campo incluye las validaciones
 * que garantizan que el correo sea obligatorio y tenga un formato válido.</p>
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email
) {
}