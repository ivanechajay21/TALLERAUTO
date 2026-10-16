package com.sigtm.backend.repository;

import com.sigtm.backend.model.SecuenciaCodigo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a datos de las secuencias de códigos correlativos.
 *
 * <p>La lectura de la secuencia se realiza con bloqueo pesimista de escritura para que dos
 * peticiones concurrentes no generen códigos duplicados.</p>
 */
@Repository
public interface SecuenciaCodigoRepository extends JpaRepository<SecuenciaCodigo, Long> {

    /**
     * Busca la secuencia de un código con bloqueo pesimista de escritura.
     *
     * <p>El bloqueo serializa el acceso concurrente a la secuencia para que dos peticiones
     * simultáneas no generen el mismo correlativo.</p>
     *
     * @param code código que identifica la secuencia.
     * @return la secuencia encontrada o vacía si el código no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SecuenciaCodigo> findByCode(String code);
}