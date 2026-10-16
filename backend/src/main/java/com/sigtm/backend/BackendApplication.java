package com.sigtm.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación backend.
 *
 * <p>Inicia el contexto de Spring Boot y habilita el escaneo automático de
 * componentes, la configuración y los repositorios del módulo.</p>
 */
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
