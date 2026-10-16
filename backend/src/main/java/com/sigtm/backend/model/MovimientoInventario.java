package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de entrada o salida de existencias del inventario.
 *
 * <p>Se persiste en la tabla movimientos_inventario y referencia siempre a un Producto y al
 * Usuario responsable de la transacción. Cada registro indica el tipo de movimiento, la cantidad,
 * el precio unitario y el documento que lo originó.</p>
 */
@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonIgnore
    private Producto product;

    @Column(name = "tipo", nullable = false, length = 10)
    private String type; // IN (Entrada), OUT (Salida)

    @Column(name = "cantidad", nullable = false)
    private Integer quantity;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "referencia", length = 100)
    private String reference; // e.g. "OrdenTrabajo: ORD-1234", "PurchaseOrder: PO-987"

    @Builder.Default
    @Column(name = "fecha_transaccion", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario user; // Person who made the transaction (storekeeper, mechanic, etc)
}