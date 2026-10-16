package com.sigtm.backend.service;

import com.sigtm.backend.exception.DuplicateResourceException;
import com.sigtm.backend.exception.ResourceNotFoundException;
import com.sigtm.backend.model.Permiso;
import com.sigtm.backend.model.Rol;
import com.sigtm.backend.model.Usuario;
import com.sigtm.backend.repository.PermisoRepository;
import com.sigtm.backend.repository.RolRepository;
import com.sigtm.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administra los roles del sistema y la asignación de permisos.
 *
 * <p>Los roles del sistema no se pueden eliminar, y el rol {@code ROLE_ADMIN} conserva
 * siempre sus permisos críticos para poder autorregular el acceso.</p>
 */
@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    private static final Set<String> SYSTEM_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_RECEPTIONIST", "ROLE_MECHANIC", "ROLE_STOREKEEPER");

    /**
     * Permisos que el rol Administrador no puede perder (autorregulan el sistema):
     * con ellos siempre podrá volver a administrar roles, asignar permisos y ver
     * usuarios aunque desactive el resto de módulos.
     */
    private static final Set<String> ADMIN_PERMISOS_IRREDUCIBLES = Set.of(
            "ROLES:VER", "ROLES:ASIGNAR", "USUARIOS:VER");

    /**
     * Lista los roles del sistema con sus permisos y el número de usuarios asignados.
     *
     * <p>Los roles internos del sistema se devuelven primero y cada registro incluye la
     * bandera {@code system} para que la interfaz impida editarlos o eliminarlos.</p>
     *
     * @return la lista de roles en formato {@code Map}
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerTodos() {
        return rolRepository.findAll().stream()
                .sorted((a, b) -> Boolean.compare(esSistema(b.getName()), esSistema(a.getName())))
                .map(this::aMapaRol)
                .toList();
    }

    /**
     * Lista todos los permisos disponibles, agrupables por módulo y submódulo.
     *
     * @return los permisos ordenados por módulo, submódulo y acción
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerPermisos() {
        return permisoRepository.findAllByOrderByModuleAscSubmoduleAscActionAsc().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("code", p.getCode());
                    m.put("module", p.getModule());
                    m.put("submodule", p.getSubmodule());
                    m.put("action", p.getAction());
                    m.put("name", p.getName());
                    return m;
                }).toList();
    }

    /**
     * Crea un nuevo rol del sistema.
     *
     * <p>El nombre es obligatorio y único, sin distinguir mayúsculas (se normaliza a
     * mayúsculas). El rol nace sin permisos y su creación se registra en la auditoría.</p>
     *
     * @param name        nombre del rol
     * @param description descripción del rol; puede ser {@code null}
     * @return el rol creado en formato {@code Map}
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     * @throws DuplicateResourceException si ya existe un rol con ese nombre
     */
    @Transactional
    public Map<String, Object> crearRol(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        String nombreRol = name.trim().toUpperCase();
        rolRepository.findByName(nombreRol)
                .ifPresent(r -> {
                    throw new DuplicateResourceException("Ya existe un rol con ese nombre");
                });
        Rol rol = new Rol();
        rol.setName(nombreRol);
        rol.setDescription(description != null ? description.trim() : null);
        rol.setPermissions(new HashSet<>());
        Rol guardado = rolRepository.save(rol);
        auditoriaService.registrar("CREATE", "ROL", guardado.getId(), "Creación del rol " + guardado.getName());
        return aMapaRol(guardado);
    }

    /**
     * Actualiza el nombre y la descripción de un rol.
     *
     * <p>Cada campo se modifica solo si llega informado; el nombre se normaliza a
     * mayúsculas y debe seguir siendo único. La operación se registra en la auditoría con
     * la acción {@code UPDATE}.</p>
     *
     * @param id          identificador del rol
     * @param name        nuevo nombre; puede ser {@code null} para no cambiarlo
     * @param description nueva descripción; puede ser {@code null} para no cambiarla
     * @return el rol actualizado en formato {@code Map}
     * @throws ResourceNotFoundException si el rol no existe
     * @throws DuplicateResourceException si el nuevo nombre ya está en uso
     */
    @Transactional
    public Map<String, Object> actualizarRol(Long id, String name, String description) {
        Rol rol = obtenerRol(id);
        if (name != null && !name.isBlank()) {
            String nombreRol = name.trim().toUpperCase();
            rolRepository.findByName(nombreRol)
                    .filter(r -> !r.getId().equals(id))
                    .ifPresent(r -> {
                        throw new DuplicateResourceException("Ya existe un rol con ese nombre");
                    });
            rol.setName(nombreRol);
        }
        if (description != null) {
            rol.setDescription(description.trim());
        }
        Rol guardado = rolRepository.save(rol);
        auditoriaService.registrar("UPDATE", "ROL", guardado.getId(), "Actualización del rol " + guardado.getName());
        return aMapaRol(guardado);
    }

    /**
     * Elimina un rol del sistema.
     *
     * <p>Los roles internos ({@code ROLE_ADMIN}, {@code ROLE_MANAGER},
     * {@code ROLE_RECEPTIONIST}, {@code ROLE_MECHANIC} y {@code ROLE_STOREKEEPER}) no se
     * pueden eliminar, ni tampoco un rol que tenga usuarios asignados. La baja se registra
     * en la auditoría con la acción {@code DELETE}.</p>
     *
     * @param id identificador del rol
     * @throws ResourceNotFoundException si el rol no existe
     * @throws IllegalArgumentException si es un rol del sistema o tiene usuarios asignados
     */
    @Transactional
    public void eliminarRol(Long id) {
        Rol rol = obtenerRol(id);
        if (esSistema(rol.getName())) {
            throw new IllegalArgumentException("Los roles del sistema no se pueden eliminar");
        }
        long usuariosAsignados = usuarioRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().getId().equals(id))
                .count();
        if (usuariosAsignados > 0) {
            throw new IllegalArgumentException("El rol está asignado a " + usuariosAsignados + " usuario(s)");
        }
        rol.setPermissions(new HashSet<>());
        rolRepository.delete(rol);
        auditoriaService.registrar("DELETE", "ROL", id, "Eliminación del rol " + rol.getName());
    }

    /**
     * Reasigna los permisos de un rol.
     *
     * <p>Para {@code ROLE_ADMIN} se re-insertan siempre los permisos críticos
     * ({@code ROLES:VER}, {@code ROLES:ASIGNAR} y {@code USUARIOS:VER}) y se registra la
     * operación en auditoría con la acción {@code PERMISSIONS}. Cualquier código que no
     * corresponda a un permiso existente se ignora.</p>
     *
     * @param id      identificador del rol
     * @param codigos códigos de permiso a asignar; puede ser {@code null} para no asignar permisos
     * @return el rol actualizado en formato {@code Map}
     * @throws ResourceNotFoundException si el rol no existe
     */
    @Transactional
    public Map<String, Object> asignarPermisos(Long id, Set<String> codigos) {
        Rol rol = obtenerRol(id);
        Set<String> finalCodigos = codigos != null ? codigos : new HashSet<>();
        if ("ROLE_ADMIN".equals(rol.getName())) {
            // El administrador ajusta sus propios módulos, pero conserva siempre los
            // permisos críticos (ver nota ADMIN_PERMISOS_IRREDUCIBLES): se evita que
            // pueda dejarse sin acceso al sistema.
            finalCodigos.addAll(ADMIN_PERMISOS_IRREDUCIBLES);
        }
        if (finalCodigos.isEmpty()) {
            rol.setPermissions(new HashSet<>());
        } else {
            List<Permiso> permissions = permisoRepository.findAll().stream()
                    .filter(p -> finalCodigos.contains(p.getCode()))
                    .toList();
            rol.setPermissions(new HashSet<>(permissions));
        }
        Rol guardado = rolRepository.save(rol);
        auditoriaService.registrar("PERMISSIONS", "ROL", guardado.getId(),
                "Actualización de permisos del rol " + guardado.getName() + " (" + guardado.getPermissions().size() + " permisos)");
        return aMapaRol(guardado);
    }

    /**
     * Obtiene los códigos de permiso del rol de un usuario.
     *
     * <p>Si el usuario no tiene rol asignado se devuelve una lista vacía.</p>
     *
     * @param userId identificador del usuario
     * @return los códigos de permiso ordenados alfabéticamente
     * @throws ResourceNotFoundException si el usuario no existe
     */
    @Transactional(readOnly = true)
    public List<String> obtenerMisPermisos(Long userId) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (user.getRole() == null) return List.of();
        return user.getRole().getPermissions().stream()
                .map(Permiso::getCode)
                .sorted()
                .toList();
    }

    private Map<String, Object> aMapaRol(Rol rol) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rol.getId());
        m.put("name", rol.getName());
        m.put("description", rol.getDescription());
        m.put("system", esSistema(rol.getName()));
        m.put("permissions", rol.getPermissions().stream()
                .map(Permiso::getCode)
                .sorted()
                .toList());
        long usuariosAsignados = usuarioRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().getId().equals(rol.getId()))
                .count();
        m.put("usersCount", usuariosAsignados);
        return m;
    }

    private Rol obtenerRol(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    private boolean esSistema(String name) {
        return SYSTEM_ROLES.contains(name);
    }
}