package com.github.dimitryivaniuta.gateway.graphql.interceptor;

import com.github.dimitryivaniuta.gateway.persistence.service.PersistedQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * WebGraphQlInterceptor that supports persisted GraphQL queries.
 *
 * <p>Protocol (simple APQ-style):
 * <ul>
 *   <li>Client sends only variables + operationName + extensions.persistedQueryId
 *       → interceptor looks up stored document by ID and injects it into the request.</li>
 *   <li>Client sends query + extensions.persistedQueryId
 *       → interceptor stores/updates mapping (ID -> query, operationName) for future use,
 *         then proceeds normally.</li>
 * </ul>
 *
 * <p>This allows UIs to cache query texts locally and only send small IDs after
 * the first round-trip.</p>
 */
@Component
public class PersistedQueryInterceptor implements WebGraphQlInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PersistedQueryInterceptor.class);

    private static final String EXT_KEY_PERSISTED_ID = "persistedQueryId";

    private final PersistedQueryService persistedQueryService;

    public PersistedQueryInterceptor(PersistedQueryService persistedQueryService) {
        this.persistedQueryService = persistedQueryService;
    }

    @Override
    @NonNull
    public Mono<ExecutionGraphQlResponse> intercept(@NonNull ExecutionGraphQlRequest request,
                                                    @NonNull Chain chain) {

        String persistedId = extractPersistedQueryId(request);
        String document = request.getDocument();
        String operationName = request.getOperationName();

        if (persistedId == null || persistedId.isBlank()) {
            // No persisted query in play – proceed as usual.
            return chain.next(request);
        }

        if (document == null || document.isBlank()) {
            // Case 1: client sent only persisted ID (no query).
            return resolvePersistedQuery(request, persistedId, operationName, chain);
        } else {
            // Case 2: client sent ID + full query – store/update it for next time.
            storePersistedQuery(persistedId, document, operationName);
            return chain.next(request);
        }
    }

    /**
     * Extract persistedQueryId from request extensions.
     */
    private String extractPersistedQueryId(ExecutionGraphQlRequest request) {
        Map<String, Object> extensions = request.getExtensions();
        if (extensions == null) {
            return null;
        }
        Object value = extensions.get(EXT_KEY_PERSISTED_ID);
        return (value instanceof String s) ? s : null;
    }

    /**
     * Resolve a persisted query (document) by ID and inject it into the request.
     */
    private Mono<ExecutionGraphQlResponse> resolvePersistedQuery(ExecutionGraphQlRequest request,
                                                                 String persistedId,
                                                                 String operationName,
                                                                 Chain chain) {
        Optional<String> docOpt = persistedQueryService.findDocumentById(persistedId);

        if (docOpt.isEmpty()) {
            log.warn("Persisted query not found for id='{}', operationName='{}'",
                    persistedId, operationName);
            // Let execution fail with standard "no query" GraphQL error.
            return chain.next(request);
        }

        String document = docOpt.get();
        log.debug("Resolved persisted query id='{}', operationName='{}'", persistedId, operationName);

        // Inject the resolved query into the execution input.
        request.configureExecutionInput((input, builder) ->
                builder.query(document).build()
        );

        return chain.next(request);
    }

    /**
     * Store or update a persisted query mapping.
     */
    private void storePersistedQuery(String persistedId,
                                     String document,
                                     String operationName) {
        try {
            persistedQueryService.saveOrUpdate(persistedId, document, operationName);
            log.debug("Stored/updated persisted query id='{}', operationName='{}'", persistedId, operationName);
        } catch (Exception ex) {
            // Persisting is best-effort – do not block the request if storage fails.
            log.warn("Failed to store persisted query id='{}': {}", persistedId, ex.getMessage());
        }
    }
}
