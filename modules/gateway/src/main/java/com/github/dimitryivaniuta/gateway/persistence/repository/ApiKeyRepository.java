package com.github.dimitryivaniuta.gateway.persistence.repository;

import com.github.dimitryivaniuta.gateway.persistence.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for looking up API keys.
 *
 * <p>Primary use: authenticate incoming API keys by token value.</p>
 */
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {

    /**
     * Find an active (enabled) API key by its public token.
     *
     * @param key raw API key string
     * @return matching enabled key if present
     */
    Optional<ApiKeyEntity> findByKeyAndEnabledIsTrue(String key);
}
