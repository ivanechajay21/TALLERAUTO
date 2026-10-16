package com.sigtm.backend.controller;

import com.sigtm.backend.exception.ResourceNotFoundException;
import com.sigtm.backend.service.RolService;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web (MVC) de {@link RolController}.
 *
 * <p>Valida el contrato HTTP y, sobre todo, la **autorización por rol**: la consulta
 * exige {@code ROLES:VER}, mientras que las operaciones de escritura (crear, actualizar,
 * eliminar roles y asignar permisos) quedan reservadas al rol {@code ADMIN}. Un usuario
 * autenticado sin ese rol recibe 403 (traducido por {@code GlobalExceptionHandler}).</p>
 */
@WebMvcTest(controllers = RolController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RolControllerTest.MethodSecurityConfig.class)
class RolControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RolService rolService;

    private static final String JSON_ROL = "{\"name\":\"Supervisor\",\"description\":\"Equipo\"}";

    // ---------------------------------------------------------------------
    // Lectura (ROLES:VER)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/roles con ROLES:VER devuelve 200")
    @WithMockUser(authorities = "ROLES:VER")
    void listar_ok() throws Exception {
        when(rolService.obtenerTodos()).thenReturn(List.of(Map.of("id", 1, "name", "ROLE_ADMIN")));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("GET /api/roles sin ROLES:VER devuelve 403")
    @WithMockUser(authorities = {})
    void listar_sinPermiso_403() throws Exception {
        mockMvc.perform(get("/api/roles")).andExpect(status().isForbidden());
        verify(rolService, never()).obtenerTodos();
    }

    // ---------------------------------------------------------------------
    // Crear (ADMIN)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/roles con rol ADMIN crea el rol (200)")
    @WithMockUser(roles = "ADMIN")
    void crear_admin_ok() throws Exception {
        when(rolService.crearRol("Supervisor", "Equipo")).thenReturn(Map.of("name", "SUPERVISOR"));

        mockMvc.perform(post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(JSON_ROL))
                .andExpect(status().isOk());

        verify(rolService).crearRol("Supervisor", "Equipo");
    }

    @Test
    @DisplayName("POST /api/roles con rol MANAGER devuelve 403")
    @WithMockUser(roles = "MANAGER")
    void crear_noAdmin_403() throws Exception {
        mockMvc.perform(post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(JSON_ROL))
                .andExpect(status().isForbidden());

        verify(rolService, never()).crearRol(any(), any());
    }

    // ---------------------------------------------------------------------
    // Actualizar (ADMIN)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/roles/{id} con ADMIN actualiza (200)")
    @WithMockUser(roles = "ADMIN")
    void actualizar_admin_ok() throws Exception {
        when(rolService.actualizarRol(eq(5L), eq("Nuevo"), any())).thenReturn(Map.of("name", "NUEVO"));

        mockMvc.perform(put("/api/roles/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo\"}"))
                .andExpect(status().isOk());

        verify(rolService).actualizarRol(eq(5L), eq("Nuevo"), any());
    }

    @Test
    @DisplayName("PUT /api/roles/{id} inexistente con ADMIN devuelve 404")
    @WithMockUser(roles = "ADMIN")
    void actualizar_noExiste_404() throws Exception {
        when(rolService.actualizarRol(eq(99L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Rol no encontrado"));

        mockMvc.perform(put("/api/roles/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/roles/{id} con MANAGER devuelve 403")
    @WithMockUser(roles = "MANAGER")
    void actualizar_noAdmin_403() throws Exception {
        mockMvc.perform(put("/api/roles/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo\"}"))
                .andExpect(status().isForbidden());

        verify(rolService, never()).actualizarRol(any(), any(), any());
    }

    // ---------------------------------------------------------------------
    // Eliminar (ADMIN)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/roles/{id} con ADMIN devuelve 204")
    @WithMockUser(roles = "ADMIN")
    void eliminar_admin_204() throws Exception {
        mockMvc.perform(delete("/api/roles/9")).andExpect(status().isNoContent());
        verify(rolService).eliminarRol(9L);
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} con MANAGER devuelve 403")
    @WithMockUser(roles = "MANAGER")
    void eliminar_noAdmin_403() throws Exception {
        mockMvc.perform(delete("/api/roles/9")).andExpect(status().isForbidden());
        verify(rolService, never()).eliminarRol(anyLong());
    }

    // ---------------------------------------------------------------------
    // Asignar permisos (ADMIN)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/roles/{id}/permisos con ADMIN asigna permisos (200)")
    @WithMockUser(roles = "ADMIN")
    void asignarPermisos_admin_ok() throws Exception {
        when(rolService.asignarPermisos(eq(1L), anySet())).thenReturn(Map.of("id", 1));

        mockMvc.perform(put("/api/roles/1/permisos").contentType(MediaType.APPLICATION_JSON)
                        .content("[\"CLIENTES:VER\"]"))
                .andExpect(status().isOk());

        verify(rolService).asignarPermisos(eq(1L), eq(Set.of("CLIENTES:VER")));
    }

    @Test
    @DisplayName("PUT /api/roles/{id}/permisos con MANAGER devuelve 403")
    @WithMockUser(roles = "MANAGER")
    void asignarPermisos_noAdmin_403() throws Exception {
        mockMvc.perform(put("/api/roles/1/permisos").contentType(MediaType.APPLICATION_JSON)
                        .content("[\"CLIENTES:VER\"]"))
                .andExpect(status().isForbidden());

        verify(rolService, never()).asignarPermisos(any(), any());
    }
}
