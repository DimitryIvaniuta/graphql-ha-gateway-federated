package com.github.dimitryivaniuta.gateway.persistence.service;

import com.github.dimitryivaniuta.gateway.persistence.entity.PersistedQueryEntity;
import com.github.dimitryivaniuta.gateway.persistence.repository.PersistedQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for storing and resolving persisted GraphQL queries.
 */
@Service
public class PersistedQueryService {

    private static final Logger log = LoggerFactory.getLogger(PersistedQueryService.class);

    private final PersistedQueryRepository repository;

    public PersistedQueryService(PersistedQueryRepository repository) {
        this.repository = repository;
    }

    /**
     * Resolve query document by persisted ID.
     */
    @Transactional(readOnly = true)
    public Optional<String> findDocumentById(String queryId) {
        return repository.findByQueryId(queryId)
                .map(PersistedQueryEntity::getDocument);
    }

    /**
     * Store or update persisted query definition.
     */
    @Transactional
    public void saveOrUpdate(String queryId, String document, String operationName) {
        PersistedQueryEntity entity = repository.findByQueryId(queryId)
                .orElseGet(() -> PersistedQueryEntity.builder()
                        .queryId(queryId)
                        .build());

        entity.setDocument(document);
        entity.setOperationName(operationName);

        repository.save(entity);
        log.debug("Persisted query stored/updated: id='{}', operationName='{}'", queryId, operationName);
    }
}

