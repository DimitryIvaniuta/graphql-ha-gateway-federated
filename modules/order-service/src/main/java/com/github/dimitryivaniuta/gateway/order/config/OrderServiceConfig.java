package com.github.dimitryivaniuta.gateway.order.config;


import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.OrderJpaEntity;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.repository.SpringDataOrderJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Order-service infrastructure configuration:
 * - scans JPA entities and repositories for the Order bounded context
 * - enables declarative transactions.
 */
@Configuration
@EnableJpaRepositories(
        basePackageClasses = SpringDataOrderJpaRepository.class
)
@EntityScan(
        basePackageClasses = OrderJpaEntity.class
)
public class OrderServiceConfig {
    // no extra beans needed for now
}