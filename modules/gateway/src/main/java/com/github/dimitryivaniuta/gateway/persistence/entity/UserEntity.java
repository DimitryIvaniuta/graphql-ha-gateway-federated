package com.github.dimitryivaniuta.gateway.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * User account used for Single Sign-On (SSO) authentication.
 *
 * <p>Stored in table {@code users} (schema is configured globally via JPA properties).</p>
 *
 * <p>Important: {@link #passwordHash} stores only a BCrypt (or similar) hash,
 * never a plain text password.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_tenant_username", columnNames = {"tenant_id", "username"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = "passwordHash")
public class UserEntity extends AbstractUuidEntity {

    /**
     * Business tenant identifier. Allows one username per tenant.
     */
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /**
     * Login name within the tenant.
     */
    @Column(name = "username", nullable = false, length = 128)
    private String username;

    /**
     * BCrypt (or similar) hash of the user password.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Comma-separated list of roles, e.g. "ROLE_USER,ROLE_ADMIN".
     */
    @Column(name = "roles", nullable = false, length = 512)
    private String roles;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @PrePersist
    void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
