package com.sigtm.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Factura o boleta emitida por el taller por los trabajos realizados.
 *
 * <p>Se persiste en la tabla facturas y está asociada a una única OrdenTrabajo, de la que
 * recoge el detalle facturado. Una factura pertenece a una orden de trabajo y su importe total
 * se descompone en subtotal e impuesto, con un estado y un método de pago asociados.</p>
 */
@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 30)
    private String invoiceNumber; // Factura o Boleta

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_trabajo_id", nullable = false)
    @JsonIgnore
    private OrdenTrabajo workOrder;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime issueDate = LocalDateTime.now();

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "monto_impuesto", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "estado", nullable = false, length = 20)
    private String status; // PENDING, PAID, CANCELLED

    @Column(name = "metodo_pago", length = 30)
    private String paymentMethod; // CASH, CARD, TRANSFER
}