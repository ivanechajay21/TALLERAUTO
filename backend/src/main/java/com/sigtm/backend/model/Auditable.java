package com.sigtm.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Clase base abstracta que incorpora las marcas de auditoría a las entidades del taller.
 *
 * <p>Es una superclase mapeada, por lo que sus cuatro columnas (creado_por, creado_en,
 * actualizado_por y actualizado_en) se agregan a cada tabla de las entidades que la extienden.
 * Los valores se completan automáticamente mediante el listener de auditoría de Spring Data.</p>
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedBy
    @Column(name = "creado_por", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "actualizado_por")
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;
}