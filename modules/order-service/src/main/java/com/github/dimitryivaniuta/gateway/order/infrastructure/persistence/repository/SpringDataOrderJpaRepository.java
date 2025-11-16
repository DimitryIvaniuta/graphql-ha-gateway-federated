package com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.repository;

import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataOrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByIdIn(Collection<UUID> ids);
}
