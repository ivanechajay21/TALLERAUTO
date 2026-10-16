package com.sigtm.backend.service;

import com.sigtm.backend.model.SecuenciaCodigo;
import com.sigtm.backend.repository.SecuenciaCodigoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera códigos correlativos y legibles por entidad, a partir de las secuencias
 * almacenadas en base de datos.
 */
@Service
@RequiredArgsConstructor
public class CodeGeneratorService {

    private final SecuenciaCodigoRepository codeSequenceRepository;

    /**
     * Genera un código secuencial con prefijo legible y nunca reutilizado.
     * Ejemplo: CLI-000001, VEH-000001, OT-000001, REC-000001.
     *
     * <p>Si la secuencia del prefijo no existe se crea con valor {@code 0} y luego se
     * incrementa dentro de la misma transacción, de modo que los códigos emitidos son
     * correlativos y gaps de una transacción revertida no se reutilizan.</p>
     *
     * @param prefix prefijo de la entidad, por ejemplo {@code CLI}, {@code VEH},
     *               {@code OT} o {@code REC}
     * @return el código generado con el formato {@code PREFIJO-000000}
     */
    @Transactional
    public String nextCode(String prefix) {
        SecuenciaCodigo sequence = codeSequenceRepository.findByCode(prefix)
                .orElseGet(() -> codeSequenceRepository.save(
                        SecuenciaCodigo.builder().code(prefix).lastValue(0L).build()));

        long next = sequence.getLastValue() + 1;
        sequence.setLastValue(next);
        codeSequenceRepository.save(sequence);

        return String.format("%s-%06d", prefix, next);
    }
}