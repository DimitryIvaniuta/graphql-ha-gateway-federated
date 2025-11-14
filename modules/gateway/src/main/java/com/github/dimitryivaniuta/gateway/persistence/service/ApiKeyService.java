package com.github.dimitryivaniuta.gateway.persistence.service;

import com.github.dimitryivaniuta.gateway.persistence.entity.ApiKeyEntity;
import com.github.dimitryivaniuta.gateway.persistence.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Domain service for API key authentication and lookup.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Normalize and validate raw API key values from HTTP headers.</li>
 *   <li>Lookup active keys from PostgreSQL via {@link ApiKeyRepository}.</li>
 *   <li>Provide a simple high-level API for filters and resolvers to consume.</li>
 * </ul>
 *
 * <p>Rate limiting / tenant resolution / metrics are intentionally kept out of this service
 * to maintain focus on authentication. They can be added in separate components.</p>
 */
@Service
@Transactional(readOnly = true)
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Authenticate an API key value by checking that:
     * <ul>
     *   <li>It is non-null and non-blank.</li>
     *   <li>It corresponds to an {@link ApiKeyEntity} that is {@code enabled=true}.</li>
     * </ul>
     *
     * @param rawKey value from HTTP header (e.g. {@code X-API-Key})
     * @return an active API key entity if authentication succeeds
     */
    public Optional<ApiKeyEntity> authenticate(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            return Optional.empty();
        }

        String normalized = rawKey.trim();
        Optional<ApiKeyEntity> result = apiKeyRepository.findByKeyAndEnabledIsTrue(normalized);

        if (result.isEmpty()) {
            log.debug("API key authentication failed: no active key found for token '{}'", normalized);
        } else {
            log.debug("API key authentication succeeded for key '{}', name='{}'",
                    normalized, result.get().getName());
        }

        return result;
    }

    /**
     * Convenience method for simple validity checks where the caller does not need the entity.
     *
     * @param rawKey raw API key value
     * @return {@code true} if an active key exists for the token
     */
    public boolean isValid(String rawKey) {
        return authenticate(rawKey).isPresent();
    }
}
