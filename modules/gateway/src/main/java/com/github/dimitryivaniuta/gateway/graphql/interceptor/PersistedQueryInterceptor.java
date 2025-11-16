package com.github.dimitryivaniuta.gateway.graphql.interceptor;

import com.github.dimitryivaniuta.gateway.service.PersistedQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Interceptor providing support for persisted GraphQL queries.
 *
 * Protocol:
 *  - First call: client sends query + extensions.persistedQueryId -> stored.
 *  - Subsequent calls: client sends only extensions.persistedQueryId -> query is looked up and injected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersistedQueryInterceptor implements WebGraphQlInterceptor {

    private static final String EXT_KEY_PERSISTED_ID = "persistedQueryId";

    private final PersistedQueryService persistedQueryService;

    @Override
    public Mono<WebGraphQlResponse> intercept(@NotNull WebGraphQlRequest request, @NotNull Chain chain) {

        String persistedId = extractPersistedQueryId(request);
        String document = request.getDocument();
        String operationName = request.getOperationName();

        if (persistedId == null || persistedId.isBlank()) {
            // No persisted-query behaviour requested.
            return chain.next(request);
        }

        if (document.isBlank()) {
            // Lookup existing persisted query and inject it.
            Optional<String> docOpt = persistedQueryService.findDocumentById(persistedId);

            if (docOpt.isEmpty()) {
                log.warn("Persisted query not found for id='{}', operationName='{}'",
                        persistedId, operationName);
                // Continue without document; engine will report error.
                return chain.next(request);
            }

            String resolvedDoc = docOpt.get();
            log.debug("Resolved persisted query id='{}', operationName='{}'", persistedId, operationName);

            request.configureExecutionInput((input, builder) ->
                    builder.query(resolvedDoc).build()
            );

            return chain.next(request);
        }

        // Document present + ID -> store or update mapping (best-effort).
        try {
            persistedQueryService.saveOrUpdate(persistedId, document, operationName);
            log.debug("Stored/updated persisted query id='{}', operationName='{}'", persistedId, operationName);
        } catch (Exception ex) {
            log.warn("Failed to store persisted query id='{}': {}", persistedId, ex.getMessage());
        }

        return chain.next(request);
    }

    private String extractPersistedQueryId(WebGraphQlRequest request) {
        Map<String, Object> extensions = request.getExtensions();
        Object value = extensions.get(EXT_KEY_PERSISTED_ID);
        return (value instanceof String s) ? s : null;
    }
}
