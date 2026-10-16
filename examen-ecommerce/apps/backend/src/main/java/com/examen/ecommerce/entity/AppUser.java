package com.examen.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Tabla app_users (solo lectura): usuarios que pueden obtener un token JWT. */
@Entity
@Table(name = "app_users")
public class AppUser extends AuditableEntity {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    /** Hash BCrypt; nunca se guarda ni se expone la contraseña en texto plano. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Rol sin prefijo ROLE_ (USER, ADMIN). */
    @Column(name = "role", nullable = false, length = 30)
    private String role;

    /** TINYINT(1) UNSIGNED → boolean (necesario para ddl-auto=validate). */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String role, boolean active) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        // Sin passwordHash a propósito
        return "AppUser{id=" + getId() + ", username='" + username + "', role=" + role + ", active=" + active + '}';
    }
}
