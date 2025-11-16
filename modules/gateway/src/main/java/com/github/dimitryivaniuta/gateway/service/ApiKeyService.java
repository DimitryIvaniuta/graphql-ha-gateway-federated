package com.github.dimitryivaniuta.gateway.service;

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
 *   <li>Normalize raw API key values coming from HTTP headers.</li>
 *   <li>Look up active (enabled) keys from PostgreSQL via {@link ApiKeyRepository}.</li>
 *   <li>Expose a simple API for filters/controllers to verify API keys.</li>
 * </ul>
 *
 * <p>Rate limiting and tenant resolution are intentionally kept out of this service.</p>
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
     * Authenticate an API key by verifying that it maps to an enabled {@link ApiKeyEntity}.
     *
     * @param rawKey value from the HTTP header (e.g. {@code X-API-Key})
     * @return an active API key entity if authentication succeeds
     */
    public Optional<ApiKeyEntity> authenticate(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            return Optional.empty();
        }

        String normalized = rawKey.trim();
        Optional<ApiKeyEntity> result = apiKeyRepository.findByKeyAndEnabledIsTrue(normalized);

        if (result.isEmpty()) {
            log.debug("API key authentication failed: no active key for token '{}'", normalized);
        } else {
            ApiKeyEntity entity = result.get();
            log.debug("API key authentication succeeded for key='{}', name='{}'",
                    normalized, entity.getName());
        }

        return result;
    }

    /**
     * Convenience method when the caller only cares about validity.
     */
    public boolean isValid(String rawKey) {
        return authenticate(rawKey).isPresent();
    }
}
