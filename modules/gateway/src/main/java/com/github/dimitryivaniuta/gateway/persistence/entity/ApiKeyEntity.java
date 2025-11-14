package com.github.dimitryivaniuta.gateway.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * Persistent representation of an API key allowed to call the gateway.
 *
 * <p>Table: {@code api_key} in schema {@code gateway} (schema is configured globally via JPA properties).</p>
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code key} – public API key token presented by clients, unique.</li>
 *   <li>{@code name} – human-readable label, e.g. tenant or application name.</li>
 *   <li>{@code enabled} – simple on/off switch.</li>
 *   <li>{@code rateLimitPerMinute} – per-key rate limit (enforced externally/in future).</li>
 *   <li>{@code createdAt} – audit timestamp (UTC).</li>
 * </ul>
 */
@Entity
@Table(
        name = "api_key",
        indexes = {
                @Index(name = "idx_api_key_key_unique", columnList = "\"key\"", unique = true)
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class ApiKeyEntity extends AbstractUuidEntity {

    @Column(name = "\"key\"", nullable = false, unique = true, length = 128)
    private String key;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 120;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Ensure {@link #createdAt} is set in UTC on insert if not provided explicitly.
     */
    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
