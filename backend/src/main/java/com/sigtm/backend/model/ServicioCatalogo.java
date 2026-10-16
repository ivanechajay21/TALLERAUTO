package com.sigtm.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Servicio ofrecido por el taller dentro de su catálogo de trabajos.
 *
 * <p>Se persiste en la tabla catalogo_servicios y ofrece un precio predeterminado y una duración
 * estimada que se usan al añadir el servicio a una orden de trabajo. El precio finalmente
 * aplicado se registra en el ítem correspondiente de la orden.</p>
 */
@Entity
@Table(name = "catalogo_servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioCatalogo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "nombre", nullable = false, length = 100)
    private String name;

    @Column(name = "descripcion", length = 255)
    private String description;

    @Column(name = "categoria", length = 50)
    private String category;

    @Column(name = "precio_predeterminado", precision = 10, scale = 2)
    private BigDecimal defaultPrice;

    @Column(name = "duracion_estimada_minutos")
    private Integer estimatedDurationMinutes;

    @Column(name = "activo", nullable = false)
    private Boolean active = true;
}