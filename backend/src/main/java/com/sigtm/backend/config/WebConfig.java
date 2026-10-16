package com.sigtm.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configuración web de la aplicación.
 *
 * <p>Habilita la publicación de los archivos subidos al directorio
 * configurado en la aplicación, de modo que puedan servirse como recursos
 * estáticos a través de la ruta {@code /uploads/**}.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Registra el manejador de recursos que expone los archivos subidos.
     *
     * @param registry Registro de manejadores de recursos de Spring MVC.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}