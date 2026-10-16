package com.sigtm.backend.service;

import com.sigtm.backend.dto.AjusteStockRequest;
import com.sigtm.backend.exception.DuplicateResourceException;
import com.sigtm.backend.exception.ResourceNotFoundException;
import com.sigtm.backend.model.Producto;
import com.sigtm.backend.repository.MovimientoInventarioRepository;
import com.sigtm.backend.repository.ProductoRepository;
import com.sigtm.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la lógica de negocio de {@link ProductoService}.
 *
 * <p>Se aíslan las dependencias con Mockito (repositorios y servicios colaboradores),
 * sin levantar el contexto de Spring ni la base de datos, para validar las reglas
 * del inventario: normalización, unicidad de código, valores por defecto, baja
 * lógica y validaciones de los movimientos de stock.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ProductoService productoService;

    /** Crea un producto de prueba con los datos indicados. */
    private Producto producto(String code, String name, String brand, String category,
                              boolean active, int stock, int minStock) {
        return Producto.builder()
                .code(code)
                .name(name)
                .brand(brand)
                .category(category)
                .active(active)
                .currentStock(stock)
                .minStock(minStock)
                .purchasePrice(new BigDecimal("10.00"))
                .salePrice(new BigDecimal("15.00"))
                .build();
    }

    // ---------------------------------------------------------------------
    // obtenerTodos
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("obtenerTodos filtra por texto libre en código, nombre, marca o categoría")
    void obtenerTodos_filtraPorTexto() {
        Producto aceite = producto("ACE-5W30", "Aceite sintético", "Castrol", "Aceites", true, 10, 5);
        Producto filtro = producto("FLT-01", "Filtro de aire", "Mann", "Filtros", true, 3, 5);
        when(productoRepository.findByActiveTrue()).thenReturn(List.of(aceite, filtro));

        List<Producto> resultado = productoService.obtenerTodos(false, "aceite");

        assertEquals(1, resultado.size());
        assertEquals("ACE-5W30", resultado.get(0).getCode());
    }

    @Test
    @DisplayName("obtenerTodos sin búsqueda devuelve todos los productos activos")
    void obtenerTodos_sinBusquedaDevuelveTodo() {
        Producto aceite = producto("ACE-5W30", "Aceite sintético", "Castrol", "Aceites", true, 10, 5);
        Producto filtro = producto("FLT-01", "Filtro de aire", "Mann", "Filtros", true, 3, 5);
        when(productoRepository.findByActiveTrue()).thenReturn(List.of(aceite, filtro));

        List<Producto> resultado = productoService.obtenerTodos(false, "  ");

        assertEquals(2, resultado.size());
    }

    @Test
    @DisplayName("obtenerTodos con incluirInactivos usa el listado ordenado por nombre")
    void obtenerTodos_incluirInactivos() {
        when(productoRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(producto("A-1", "Alpha", null, null, false, 0, 5)));

        List<Producto> resultado = productoService.obtenerTodos(true, null);

        assertEquals(1, resultado.size());
        verify(productoRepository).findAll(any(Sort.class));
        verify(productoRepository, never()).findByActiveTrue();
    }

    // ---------------------------------------------------------------------
    // obtenerPorId
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("obtenerPorId lanza ResourceNotFoundException si no existe")
    void obtenerPorId_noExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerPorId(99L));
    }

    // ---------------------------------------------------------------------
    // crearProducto
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("crearProducto exige código (IllegalArgumentException)")
    void crearProducto_codigoObligatorio() {
        Producto sinCodigo = new Producto();
        sinCodigo.setName("Bujía");

        assertThrows(IllegalArgumentException.class, () -> productoService.crearProducto(sinCodigo));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    @DisplayName("crearProducto rechaza un código ya existente (DuplicateResourceException)")
    void crearProducto_codigoDuplicado() {
        Producto nuevo = producto("ACE-5W30", "Aceite", "Castrol", "Aceites", true, 0, 5);
        when(productoRepository.findByCodeIgnoreCase("ACE-5W30"))
                .thenReturn(Optional.of(new Producto()));

        assertThrows(DuplicateResourceException.class, () -> productoService.crearProducto(nuevo));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    @DisplayName("crearProducto aplica valores por defecto (stock 0, mínimo 5, activo) y audita")
    void crearProducto_valoresPorDefecto() {
        Producto nuevo = new Producto();
        nuevo.setCode("NEW-01");
        nuevo.setName("Bujía de encendido");
        when(productoRepository.findByCodeIgnoreCase("NEW-01")).thenReturn(Optional.empty());
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto guardado = productoService.crearProducto(nuevo);

        assertEquals(0, guardado.getCurrentStock());
        assertEquals(5, guardado.getMinStock());
        assertTrue(guardado.getActive());
        assertEquals("NEW-01", guardado.getCode());
        verify(auditoriaService).registrar(eq("CREATE"), eq("PRODUCTO"), any(), anyString());
        // Stock inicial 0 -> no debe registrar movimiento de inventario.
        verify(movimientoInventarioRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // actualizarProducto
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("actualizarProducto rechaza cambiar a un código ya ocupado")
    void actualizarProducto_codigoDuplicado() {
        Producto existente = producto("OLD-01", "Antiguo", null, null, true, 5, 5);
        existente.setId(1L);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(existente));

        Producto detalles = new Producto();
        detalles.setCode("DUP-01");
        when(productoRepository.findByCodeIgnoreCase("DUP-01")).thenReturn(Optional.of(new Producto()));

        assertThrows(DuplicateResourceException.class,
                () -> productoService.actualizarProducto(1L, detalles));
    }

    // ---------------------------------------------------------------------
    // eliminarProducto
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("eliminarProducto hace baja lógica (desactiva) y audita")
    void eliminarProducto_desactiva() {
        Producto existente = producto("X-01", "Producto", null, null, true, 4, 5);
        existente.setId(7L);
        when(productoRepository.findById(7L)).thenReturn(Optional.of(existente));

        productoService.eliminarProducto(7L);

        assertFalse(existente.getActive());
        verify(productoRepository).save(existente);
        verify(auditoriaService).registrar(eq("DELETE"), eq("PRODUCTO"), eq(7L), anyString());
    }

    // ---------------------------------------------------------------------
    // aplicarCambioStock (validaciones previas, sin tocar repositorios)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("aplicarCambioStock rechaza solicitud nula")
    void aplicarCambioStock_solicitudNula() {
        assertThrows(IllegalArgumentException.class, () -> productoService.aplicarCambioStock(1L, null));
        verify(productoRepository, never()).findWithLockingById(any());
    }

    @Test
    @DisplayName("aplicarCambioStock rechaza cantidad menor a 1")
    void aplicarCambioStock_cantidadInvalida() {
        AjusteStockRequest solicitud = new AjusteStockRequest(0, "IN", null);

        assertThrows(IllegalArgumentException.class, () -> productoService.aplicarCambioStock(1L, solicitud));
        verify(productoRepository, never()).findWithLockingById(any());
    }

    @Test
    @DisplayName("aplicarCambioStock rechaza un tipo distinto de IN/OUT")
    void aplicarCambioStock_tipoInvalido() {
        AjusteStockRequest solicitud = new AjusteStockRequest(5, "XX", null);

        assertThrows(IllegalArgumentException.class, () -> productoService.aplicarCambioStock(1L, solicitud));
        verify(productoRepository, never()).findWithLockingById(any());
    }
}
