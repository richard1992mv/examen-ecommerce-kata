package com.examen.ecommerce.security;

import com.examen.ecommerce.entity.AppUser;
import com.examen.ecommerce.repository.AppUserRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los usuarios desde la tabla app_users de MySQL.
 * El hash BCrypt se compara en DaoAuthenticationProvider; un usuario con is_active = 0
 * queda deshabilitado y el login responde 401.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public DatabaseUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        AppUser user = appUserRepository.findByUsername(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().trim().toUpperCase(Locale.ROOT))
                .disabled(!user.isActive())
                .build();
    }
}
