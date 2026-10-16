package com.sigtm.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Repuesto o artículo del inventario del taller.
 *
 * <p>Se persiste en la tabla productos con su código, sus precios de compra y venta y los
 * niveles de stock actual y mínimo. El stock mínimo sirve de referencia para detectar cuándo
 * un producto debe reponerse y generar la correspondiente alerta de inventario.</p>
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede superar 50 caracteres")
    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String code; // SKU/Código de barras

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Column(name = "nombre", nullable = false, length = 150)
    private String name;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    @Column(name = "descripcion", length = 255)
    private String description;

    @Size(max = 50, message = "La marca no puede superar 50 caracteres")
    @Column(name = "marca", length = 50)
    private String brand;

    @Size(max = 50, message = "La categoría no puede superar 50 caracteres")
    @Column(name = "categoria", length = 50)
    private String category;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio de compra no puede ser negativo")
    @Column(name = "precio_compra", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Builder.Default
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(name = "stock_actual", nullable = false)
    private Integer currentStock = 0;

    @Builder.Default
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Column(name = "stock_minimo", nullable = false)
    private Integer minStock = 5;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean active = true;
}