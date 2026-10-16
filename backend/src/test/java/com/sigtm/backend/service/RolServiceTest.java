package com.sigtm.backend.service;

import com.sigtm.backend.exception.DuplicateResourceException;
import com.sigtm.backend.exception.ResourceNotFoundException;
import com.sigtm.backend.model.Permiso;
import com.sigtm.backend.model.Rol;
import com.sigtm.backend.model.Usuario;
import com.sigtm.backend.repository.PermisoRepository;
import com.sigtm.backend.repository.RolRepository;
import com.sigtm.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
 * Pruebas unitarias de {@link RolService}.
 *
 * <p>Cubren las reglas de negocio de roles y permisos, con especial énfasis en la
 * salvaguarda del rol {@code ROLE_ADMIN}: aunque se le asigne un conjunto de permisos
 * vacío o sin los críticos, debe conservar siempre {@code ROLES:VER},
 * {@code ROLES:ASIGNAR} y {@code USUARIOS:VER} para poder autorregular el acceso.</p>
 */
@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;
    @Mock
    private PermisoRepository permisoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private RolService rolService;

    private Permiso permiso(String code) {
        return Permiso.builder().code(code).module("M").submodule("S").action("VER").name(code).build();
    }

    private Rol rol(Long id, String name) {
        return Rol.builder().id(id).name(name).permissions(new HashSet<>()).build();
    }

    private Set<String> codigosDe(Rol rol) {
        return rol.getPermissions().stream().map(Permiso::getCode).collect(Collectors.toSet());
    }

    // ---------------------------------------------------------------------
    // asignarPermisos — salvaguarda del ADMIN
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("asignarPermisos al ADMIN conserva los permisos críticos aunque no se listen")
    void asignarPermisos_adminMantienePermisosCriticos() {
        Rol admin = rol(1L, "ROLE_ADMIN");
        when(rolRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(permisoRepository.findAll()).thenReturn(List.of(
                permiso("ROLES:VER"), permiso("ROLES:ASIGNAR"), permiso("USUARIOS:VER"),
                permiso("AUDITORIA:VER")));
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> inv.getArgument(0));

        // Se intenta dejar al admin solo con AUDITORIA:VER.
        rolService.asignarPermisos(1L, new HashSet<>(Set.of("AUDITORIA:VER")));

        Set<String> codigos = codigosDe(admin);
        assertTrue(codigos.containsAll(Set.of("ROLES:VER", "ROLES:ASIGNAR", "USUARIOS:VER")),
                "El admin debe conservar los permisos irreducibles");
        assertTrue(codigos.contains("AUDITORIA:VER"));
    }

    @Test
    @DisplayName("asignarPermisos al ADMIN con lista VACÍA conserva exactamente los críticos")
    void asignarPermisos_adminConListaVaciaConservaCriticos() {
        Rol admin = rol(1L, "ROLE_ADMIN");
        when(rolRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(permisoRepository.findAll()).thenReturn(List.of(
                permiso("ROLES:VER"), permiso("ROLES:ASIGNAR"), permiso("USUARIOS:VER")));
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> inv.getArgument(0));

        rolService.asignarPermisos(1L, new HashSet<>());

        assertEquals(Set.of("ROLES:VER", "ROLES:ASIGNAR", "USUARIOS:VER"), codigosDe(admin));
    }

    @Test
    @DisplayName("asignarPermisos a un rol NO admin respeta exactamente la lista indicada")
    void asignarPermisos_noAdminRespetaLista() {
        Rol manager = rol(2L, "ROLE_MANAGER");
        when(rolRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(permisoRepository.findAll()).thenReturn(List.of(
                permiso("CLIENTES:VER"), permiso("ROLES:VER")));
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> inv.getArgument(0));

        rolService.asignarPermisos(2L, new HashSet<>(Set.of("CLIENTES:VER")));

        assertEquals(Set.of("CLIENTES:VER"), codigosDe(manager));
    }

    @Test
    @DisplayName("asignarPermisos lanza ResourceNotFoundException si el rol no existe")
    void asignarPermisos_rolNoExiste() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> rolService.asignarPermisos(99L, new HashSet<>()));
    }

    // ---------------------------------------------------------------------
    // crearRol
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("crearRol exige nombre")
    void crearRol_nombreObligatorio() {
        assertThrows(IllegalArgumentException.class, () -> rolService.crearRol("   ", null));
        verify(rolRepository, never()).save(any(Rol.class));
    }

    @Test
    @DisplayName("crearRol rechaza nombre duplicado (sin distinguir mayúsculas)")
    void crearRol_nombreDuplicado() {
        when(rolRepository.findByName("SUPERVISOR")).thenReturn(Optional.of(rol(3L, "SUPERVISOR")));

        assertThrows(DuplicateResourceException.class, () -> rolService.crearRol("supervisor", null));
        verify(rolRepository, never()).save(any(Rol.class));
    }

    @Test
    @DisplayName("crearRol normaliza el nombre, nace sin permisos y audita")
    void crearRol_ok() {
        when(rolRepository.findByName("SUPERVISOR")).thenReturn(Optional.empty());
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> creado = rolService.crearRol(" supervisor ", "  Equipo ");

        assertEquals("SUPERVISOR", creado.get("name"));
        assertEquals("Equipo", creado.get("description"));
        assertFalse((Boolean) creado.get("system"));
        assertEquals(List.of(), creado.get("permissions"));
        verify(auditoriaService).registrar(eq("CREATE"), eq("ROL"), any(), anyString());
    }

    // ---------------------------------------------------------------------
    // eliminarRol
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("eliminarRol no permite borrar un rol del sistema")
    void eliminarRol_rolDeSistema() {
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol(1L, "ROLE_ADMIN")));

        assertThrows(IllegalArgumentException.class, () -> rolService.eliminarRol(1L));
        verify(rolRepository, never()).delete(any(Rol.class));
    }

    @Test
    @DisplayName("eliminarRol no permite borrar un rol con usuarios asignados")
    void eliminarRol_conUsuarios() {
        Rol personalizado = rol(5L, "ROLE_X");
        when(rolRepository.findById(5L)).thenReturn(Optional.of(personalizado));
        when(usuarioRepository.findAll())
                .thenReturn(List.of(Usuario.builder().id(9L).role(personalizado).build()));

        assertThrows(IllegalArgumentException.class, () -> rolService.eliminarRol(5L));
        verify(rolRepository, never()).delete(any(Rol.class));
    }

    @Test
    @DisplayName("eliminarRol borra un rol libre y audita")
    void eliminarRol_ok() {
        Rol personalizado = rol(6L, "ROLE_Y");
        when(rolRepository.findById(6L)).thenReturn(Optional.of(personalizado));
        when(usuarioRepository.findAll()).thenReturn(List.of());

        rolService.eliminarRol(6L);

        verify(rolRepository).delete(personalizado);
        verify(auditoriaService).registrar(eq("DELETE"), eq("ROL"), eq(6L), anyString());
    }

    // ---------------------------------------------------------------------
    // obtenerMisPermisos
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("obtenerMisPermisos devuelve vacío si el usuario no tiene rol")
    void obtenerMisPermisos_sinRol() {
        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(Usuario.builder().id(1L).role(null).build()));

        assertEquals(List.of(), rolService.obtenerMisPermisos(1L));
    }

    @Test
    @DisplayName("obtenerMisPermisos lanza ResourceNotFoundException si el usuario no existe")
    void obtenerMisPermisos_usuarioNoExiste() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rolService.obtenerMisPermisos(7L));
    }

    @Test
    @DisplayName("obtenerMisPermisos devuelve los códigos ordenados alfabéticamente")
    void obtenerMisPermisos_ordenados() {
        Rol rol = Rol.builder().id(2L).name("ROLE_M")
                .permissions(new HashSet<>(Set.of(permiso("VEHICULOS:VER"), permiso("CLIENTES:VER"))))
                .build();
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(Usuario.builder().id(2L).role(rol).build()));

        assertEquals(List.of("CLIENTES:VER", "VEHICULOS:VER"), rolService.obtenerMisPermisos(2L));
    }
}
