package com.sigtm.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuración de auditoría de entidades JPA.
 *
 * <p>Habilita el llenado automático de los campos de auditoría tomando como
 * auditor al usuario autenticado en el contexto de seguridad. Cuando la
 * operación no procede de un usuario autenticado, por ejemplo tareas
 * programadas o peticiones anónimas, se registra {@code SYSTEM} como
 * responsable.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Proporciona el nombre del auditor para las entidades auditadas.
     *
     * @return Proveedor que resuelve el usuario autenticado, o
     *         {@code SYSTEM} si no hay una autenticación válida.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(authentication.getName());
        };
    }
}
