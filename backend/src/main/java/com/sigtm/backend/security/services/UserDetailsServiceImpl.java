package com.sigtm.backend.security.services;

import com.sigtm.backend.model.Usuario;
import com.sigtm.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de carga de detalles de usuario para Spring Security.
 *
 * <p>Busca el usuario por su nombre de usuario en el repositorio y lo
 * convierte en un {@link com.sigtm.backend.security.services.UserDetailsImpl}
 * con sus roles y permisos. Si no existe, lanza la excepción que la
 * infraestructura de seguridad interpreta como credenciales inválidas.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository userRepository;

    /**
     * Busca un usuario por su nombre de usuario y devuelve sus detalles de
     * seguridad.
     *
     * @param username Nombre de usuario a buscar.
     * @return Detalles del usuario encontrado.
     * @throws UsernameNotFoundException Si no existe ningún usuario con ese
     *                                  nombre de usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el nombre de usuario: " + username));

        return UserDetailsImpl.build(user);
    }
}
