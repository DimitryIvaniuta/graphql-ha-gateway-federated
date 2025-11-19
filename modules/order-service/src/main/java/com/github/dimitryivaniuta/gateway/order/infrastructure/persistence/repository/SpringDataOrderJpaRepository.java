package com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.repository;

import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Low-level Spring Data JPA repository for OrderJpaEntity.
 * <p>
 * Not exposed outside infrastructure; domain code uses the port
 * com.github.dimitryivaniuta.gateway.order.domain.OrderRepository.
 */
public interface SpringDataOrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByIdIn(Collection<UUID> ids);

    Page<OrderJpaEntity> findAll(Pageable pageable);
}
