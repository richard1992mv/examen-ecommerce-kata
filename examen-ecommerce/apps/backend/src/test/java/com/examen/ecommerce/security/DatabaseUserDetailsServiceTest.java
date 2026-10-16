package com.examen.ecommerce.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.entity.AppUser;
import com.examen.ecommerce.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DisplayName("DatabaseUserDetailsService · usuarios desde la tabla app_users (unitario)")
class DatabaseUserDetailsServiceTest {

    private static final String ADMIN_HASH = "$2a$10$M6EUsAoIsb.FqakWs3EA9Oy0Xk6buEkxEvmuyWxKLqebYGBPBI1uS";

    private AppUserRepository repository;
    private DatabaseUserDetailsService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppUserRepository.class);
        service = new DatabaseUserDetailsService(repository);
    }

    @Test
    @DisplayName("Usuario activo → UserDetails con hash BCrypt y rol ROLE_ADMIN")
    void loadsActiveUser() {
        when(repository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", ADMIN_HASH, "ADMIN", true)));

        UserDetails user = service.loadUserByUsername("admin");

        assertEquals("admin", user.getUsername());
        assertEquals(ADMIN_HASH, user.getPassword());
        assertTrue(user.isEnabled());
        assertTrue(user.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
    }

    @Test
    @DisplayName("El hash del seed corresponde a la contraseña admin123")
    void seedHashMatchesPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        assertTrue(encoder.matches("admin123", ADMIN_HASH));
        assertFalse(encoder.matches("otra", ADMIN_HASH));
    }

    @Test
    @DisplayName("Usuario con is_active = 0 → UserDetails deshabilitado")
    void inactiveUserIsDisabled() {
        when(repository.findByUsername("bloqueado"))
                .thenReturn(Optional.of(new AppUser("bloqueado", ADMIN_HASH, "USER", false)));

        assertFalse(service.loadUserByUsername("bloqueado").isEnabled());
    }

    @Test
    @DisplayName("Usuario inexistente → UsernameNotFoundException")
    void unknownUserThrows() {
        when(repository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nadie"));
    }

    @Test
    @DisplayName("El username se busca sin espacios al inicio ni al final")
    void trimsUsername() {
        when(repository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", ADMIN_HASH, "ADMIN", true)));

        assertEquals("admin", service.loadUserByUsername("  admin ").getUsername());
    }
}
