package com.sigtm.backend.service;

import com.sigtm.backend.model.Factura;
import com.sigtm.backend.model.OrdenTrabajo;
import com.sigtm.backend.model.ItemOrdenTrabajo;
import com.sigtm.backend.repository.FacturaRepository;
import com.sigtm.backend.repository.OrdenTrabajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Emite y registra el cobro de las facturas asociadas a las órdenes de trabajo.
 *
 * <p>Los importes se calculan a partir de los ítems de la orden aplicando un IGV del
 * 18 %, y toda emisión o pago queda registrado en la auditoría.</p>
 */
@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Lista todas las facturas emitidas, con el resumen de la orden de trabajo asociada.
     *
     * @return la colección de facturas en formato {@code Map}
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerTodas() {
        return facturaRepository.findAll().stream().map(this::aMapa).toList();
    }

    /**
     * Obtiene una factura por su identificador.
     *
     * <p>Si la factura no existe se lanza {@code RuntimeException} con el mensaje
     * «Factura no encontrada».</p>
     *
     * @param id identificador de la factura
     * @return la factura solicitada en formato {@code Map}
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPorId(Long id) {
        return aMapa(facturaRepository.findById(id).orElseThrow(() -> new RuntimeException("Factura no encontrada")));
    }

    /**
     * Emite la factura de una orden de trabajo.
     *
     * <p>Solo pueden facturarse órdenes en estado {@code FINALIZADO} o {@code ENTREGADO}
     * y una única vez por orden. Los importes se calculan sumando precio por cantidad de
     * cada ítem y aplicando un IGV del 18 %; la factura nace en estado {@code PENDING}
     * con un número aleatorio. Si la orden no existe, no está lista o ya fue facturada se
     * lanza {@code RuntimeException}. La emisión se registra en la auditoría.</p>
     *
     * @param workOrderId identificador de la orden de trabajo
     * @return la factura emitida en formato {@code Map}
     */
    @Transactional
    public Map<String, Object> generarFactura(Long workOrderId) {
        OrdenTrabajo workOrder = ordenTrabajoRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Orden de trabajo no encontrada"));

        if (!"FINALIZADO".equals(workOrder.getStatus()) && !"ENTREGADO".equals(workOrder.getStatus())) {
            throw new RuntimeException("La orden debe estar FINALIZADO o ENTREGADO para facturar");
        }

        if (facturaRepository.findByWorkOrderId(workOrderId).isPresent()) {
            throw new RuntimeException("Esta orden ya tiene una factura emitida");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemOrdenTrabajo item : workOrder.getItems()) {
            BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal taxRate = new BigDecimal("0.18"); // 18% IGV
        BigDecimal taxAmount = subtotal.multiply(taxRate);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        Factura factura = Factura.builder()
                .invoiceNumber("INV-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .workOrder(workOrder)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .status("PENDING")
                .build();

        Factura guardada = facturaRepository.save(factura);
        auditoriaService.registrar("CREATE", "FACTURA", guardada.getId(),
                "Emisión de factura " + guardada.getInvoiceNumber() + " para la orden " + workOrder.getOrderNumber()
                        + " (total S/ " + guardada.getTotalAmount() + ")");
        return aMapa(guardada);
    }

    /**
     * Registra el pago de una factura.
     *
     * <p>La factura pasa al estado {@code PAID} y se guarda el método de pago indicado.
     * Si la factura no existe se lanza {@code RuntimeException}. El cobro se registra en
     * la auditoría con la acción {@code PAYMENT}.</p>
     *
     * @param id            identificador de la factura
     * @param paymentMethod método de pago empleado
     * @return la factura pagada en formato {@code Map}
     */
    @Transactional
    public Map<String, Object> pagarFactura(Long id, String paymentMethod) {
        Factura factura = facturaRepository.findById(id).orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        factura.setStatus("PAID");
        factura.setPaymentMethod(paymentMethod);
        Factura guardada = facturaRepository.save(factura);
        auditoriaService.registrar("PAYMENT", "FACTURA", guardada.getId(),
                "Pago de factura " + guardada.getInvoiceNumber() + " por " + paymentMethod
                        + " (total S/ " + guardada.getTotalAmount() + ")");
        return aMapa(guardada);
    }

    private Map<String, Object> aMapa(Factura factura) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", factura.getId());
        m.put("invoiceNumber", factura.getInvoiceNumber());
        m.put("issueDate", factura.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        m.put("subtotal", factura.getSubtotal());
        m.put("taxAmount", factura.getTaxAmount());
        m.put("totalAmount", factura.getTotalAmount());
        m.put("status", factura.getStatus());
        m.put("paymentMethod", factura.getPaymentMethod());

        OrdenTrabajo ot = factura.getWorkOrder();
        if (ot != null) {
            Map<String, Object> datosOrden = new HashMap<>();
            datosOrden.put("id", ot.getId());
            datosOrden.put("orderNumber", ot.getOrderNumber());
            if (ot.getCustomer() != null) {
                m.put("customer", ot.getCustomer().getFirstName() + " " + ot.getCustomer().getLastName());
                m.put("documentNumber", ot.getCustomer().getDocumentNumber());
            }
            if (ot.getVehicle() != null) {
                m.put("vehicle", ot.getVehicle().getPlate() + " · " + ot.getVehicle().getBrand() + " " + ot.getVehicle().getModel());
            }
            m.put("workOrder", datosOrden);
        }
        return m;
    }
}
