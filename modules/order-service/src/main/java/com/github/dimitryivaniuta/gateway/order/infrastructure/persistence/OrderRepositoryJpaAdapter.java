package com.github.dimitryivaniuta.gateway.order.infrastructure.persistence;

import com.github.dimitryivaniuta.gateway.order.domain.Order;
import com.github.dimitryivaniuta.gateway.order.domain.OrderId;
import com.github.dimitryivaniuta.gateway.order.domain.OrderRepository;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.OrderJpaEntity;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.mapper.OrderEntityMapper;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.repository.SpringDataOrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryJpaAdapter implements OrderRepository {

    private final SpringDataOrderJpaRepository delegate;

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId id) {
        UUID uuid = OrderEntityMapper.toUuid(id);
        return delegate.findById(uuid).map(OrderEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByIds(Collection<OrderId> ids) {
        List<UUID> uuids = ids.stream()
                .map(OrderEntityMapper::toUuid)
                .toList();
        return delegate.findByIdIn(uuids).stream()
                .map(OrderEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity entity = OrderEntityMapper.toEntity(order);
        OrderJpaEntity saved = delegate.save(entity);
        return OrderEntityMapper.toDomain(saved);
    }
}

