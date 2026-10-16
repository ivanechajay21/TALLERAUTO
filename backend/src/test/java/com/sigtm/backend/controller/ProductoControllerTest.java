package com.sigtm.backend.controller;

import com.sigtm.backend.exception.ResourceNotFoundException;
import com.sigtm.backend.model.Producto;
import com.sigtm.backend.service.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web (MVC) de {@link ProductoController}.
 *
 * <p>Se levanta solo el contexto web con MockMvc (sin base de datos) y se simula el
 * servicio. Se valida el contrato HTTP (códigos 200/204/400/403/404), la validación
 * de Bean Validation y, habilitando method security, la **autorización por permiso**
 * ({@code @PreAuthorize}): una petición sin el permiso requerido responde 403.</p>
 */
@WebMvcTest(controllers = ProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProductoControllerTest.MethodSecurityConfig.class)
class ProductoControllerTest {

    /** Habilita la seguridad por método para que {@code @PreAuthorize} se evalúe en las pruebas. */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    private Producto producto() {
        return Producto.builder()
                .id(1L)
                .code("P1")
                .name("Filtro de aire")
                .purchasePrice(new BigDecimal("10.00"))
                .salePrice(new BigDecimal("15.00"))
                .build();
    }

    private static final String JSON_VALIDO =
            "{\"code\":\"P1\",\"name\":\"Filtro de aire\",\"purchasePrice\":10,\"salePrice\":15}";

    // ---------------------------------------------------------------------
    // Contrato HTTP
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/productos devuelve 200 y la lista")
    @WithMockUser(authorities = "PRODUCTOS:VER")
    void listar_ok() throws Exception {
        when(productoService.obtenerTodos(false, null)).thenReturn(List.of(producto()));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("P1"));
    }

    @Test
    @DisplayName("GET /api/productos propaga los filtros al servicio")
    @WithMockUser(authorities = "PRODUCTOS:VER")
    void listar_conFiltros() throws Exception {
        when(productoService.obtenerTodos(true, "ace")).thenReturn(List.of());

        mockMvc.perform(get("/api/productos").param("incluirInactivos", "true").param("search", "ace"))
                .andExpect(status().isOk());

        verify(productoService).obtenerTodos(true, "ace");
    }

    @Test
    @DisplayName("GET /api/productos/{id} devuelve 200")
    @WithMockUser(authorities = "PRODUCTOS:VER")
    void detalle_ok() throws Exception {
        when(productoService.obtenerPorId(5L)).thenReturn(producto());

        mockMvc.perform(get("/api/productos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("P1"));
    }

    @Test
    @DisplayName("GET /api/productos/{id} inexistente devuelve 404")
    @WithMockUser(authorities = "PRODUCTOS:VER")
    void detalle_noExiste_404() throws Exception {
        when(productoService.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        mockMvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/productos válido delega en el servicio (200)")
    @WithMockUser(authorities = "PRODUCTOS:CREAR")
    void crear_ok() throws Exception {
        when(productoService.crearProducto(any(Producto.class))).thenReturn(producto());

        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());

        verify(productoService).crearProducto(any(Producto.class));
    }

    @Test
    @DisplayName("POST /api/productos inválido (sin código) devuelve 400 y no llama al servicio")
    @WithMockUser(authorities = "PRODUCTOS:CREAR")
    void crear_invalido_400() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sin código\"}"))
                .andExpect(status().isBadRequest());

        verify(productoService, never()).crearProducto(any(Producto.class));
    }

    @Test
    @DisplayName("DELETE /api/productos/{id} devuelve 204")
    @WithMockUser(authorities = "PRODUCTOS:ELIMINAR")
    void eliminar_204() throws Exception {
        mockMvc.perform(delete("/api/productos/3"))
                .andExpect(status().isNoContent());

        verify(productoService).eliminarProducto(3L);
    }

    @Test
    @DisplayName("POST /api/productos/{id}/existencia con cantidad inválida devuelve 400")
    @WithMockUser(authorities = "MOVIMIENTOS:CREAR")
    void ajustarStock_invalido_400() throws Exception {
        mockMvc.perform(post("/api/productos/1/existencia").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0,\"type\":\"IN\"}"))
                .andExpect(status().isBadRequest());

        verify(productoService, never()).aplicarCambioStock(any(), any());
    }

    // ---------------------------------------------------------------------
    // Autorización (@PreAuthorize)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/productos sin permiso PRODUCTOS:VER devuelve 403")
    @WithMockUser(authorities = {})
    void listar_sinPermiso_403() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isForbidden());

        verify(productoService, never()).obtenerTodos(anyBoolean(), any());
    }

    @Test
    @DisplayName("POST /api/productos con permiso equivocado devuelve 403")
    @WithMockUser(authorities = "PRODUCTOS:VER")
    void crear_sinPermiso_403() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden());

        verify(productoService, never()).crearProducto(any(Producto.class));
    }
}
