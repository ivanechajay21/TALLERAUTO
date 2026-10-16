package com.sigtm.backend.repository;

import com.sigtm.backend.model.TokenResetContrasena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a datos de los tokens de restablecimiento de contraseña.
 *
 * <p>El token es único y se resuelve para comprobar que sigue vigente antes de permitir
 * el cambio de contraseña.</p>
 */
@Repository
public interface TokenResetContrasenaRepository extends JpaRepository<TokenResetContrasena, Long> {
    /**
     * Busca un token de restablecimiento por su valor.
     *
     * @param token valor del token enviado al usuario.
     * @return el token encontrado o vacío si no existe.
     */
    Optional<TokenResetContrasena> findByToken(String token);
}