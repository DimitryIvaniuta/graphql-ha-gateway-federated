package com.github.dimitryivaniuta.gateway.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Persisted GraphQL query text identified by a stable ID (hash or client-provided key).
 */
@Entity
@Table(
        name = "persisted_query",
        indexes = {
                @Index(name = "idx_persisted_query_id_unique", columnList = "query_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersistedQueryEntity extends AbstractUuidEntity {

    /**
     * External identifier for the query (e.g. hash or client-supplied ID).
     */
    @Column(name = "query_id", nullable = false, unique = true, length = 128)
    private String queryId;

    @Column(name = "document", nullable = false, columnDefinition = "text")
    private String document;

    @Column(name = "operation_name", length = 128)
    private String operationName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "use_count", nullable = false)
    private long useCount;

    @PrePersist
    void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastUsedAt == null) {
            lastUsedAt = now;
        }
        if (useCount == 0) {
            useCount = 1;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        lastUsedAt = OffsetDateTime.now();
        useCount = useCount + 1;
    }
}
