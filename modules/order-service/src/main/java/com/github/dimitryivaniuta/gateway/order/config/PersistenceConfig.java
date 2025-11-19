package com.github.dimitryivaniuta.gateway.order.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Persistence configuration for the order-service.
 * <p>
 * - Enables JPA auditing so entities with @CreatedBy/@LastModifiedBy
 * can be filled from the authenticated principal (SSO/JWT subject).
 * - Forces Hibernate default schema to "order" so it uses the correct
 * schema in the shared PostgreSQL instance.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class PersistenceConfig {

    /**
     * Auditor provider using the Spring Security context (JWT subject / username).
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            String name = authentication.getName();
            return (name == null || name.isBlank()) ? Optional.empty() : Optional.of(name);
        };
    }

    /**
     * Ensure Hibernate uses the "order" schema in the shared Postgres database.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return props -> props.put(AvailableSettings.DEFAULT_SCHEMA, "order");
    }
}
