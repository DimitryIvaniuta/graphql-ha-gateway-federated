package com.github.dimitryivaniuta.gateway.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Base class for JPA entities that use an application-generated {@link UUID}
 * as primary key.
 *
 * <p>Characteristics:
 * <ul>
 *   <li>ID is generated in the application via Hibernate's UUID generator.</li>
 *   <li>Mapped to PostgreSQL {@code uuid} column (16 bytes).</li>
 *   <li>No DB sequence or identity column required, so no extra round-trip
 *       is needed to obtain the identifier.</li>
 *   <li>Includes optimistic locking via {@link #version}.</li>
 * </ul>
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@ToString(exclude = "version")
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractUuidEntity {

    /**
     * Primary key, generated in the JVM.
     *
     * <p>In Hibernate 6, using {@link UUID} type with {@link GeneratedValue}
     * automatically uses {@code UUIDGenerator}, which generates the value
     * on the application side before INSERT.</p>
     */
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Optimistic locking column. Hibernate increments it on each successful update,
     * protecting against lost updates in concurrent scenarios.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
