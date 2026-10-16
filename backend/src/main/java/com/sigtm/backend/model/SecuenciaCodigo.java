package com.sigtm.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Contador utilizado para generar los códigos correlativos de las entidades del taller.
 *
 * <p>Se persiste en la tabla secuencias_codigo con un registro por tipo de código (CLI, VEH, OT,
 * REC). Cada fila guarda el último valor emitido, de modo que la numeración pueda continuar sin
 * repetir códigos.</p>
 */
@Entity
@Table(name = "secuencias_codigo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuenciaCodigo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String code; // CLI, VEH, OT, REC

    @Column(name = "ultimo_valor", nullable = false)
    private Long lastValue = 0L;
}