package com.github.dimitryivaniuta.gateway.web.error;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Central GraphQL error handler for the gateway.
 *
 * <p>Transforms Java exceptions thrown from resolvers into GraphQL errors
 * with consistent extensions and logging.</p>
 */
@Slf4j
@Controller
public class GraphQlErrorHandler {

    @GraphQlExceptionHandler(IllegalArgumentException.class)
    public GraphQLError handleIllegalArgument(IllegalArgumentException ex,
                                              DataFetchingEnvironment env) {
        log.debug("GraphQL BAD_REQUEST at {}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage(), ex);
        return buildError(ex.getMessage(), env, ErrorType.BAD_REQUEST, "BAD_REQUEST");
    }

    @GraphQlExceptionHandler(RuntimeException.class)
    public GraphQLError handleRuntimeException(RuntimeException ex,
                                               DataFetchingEnvironment env) {
        log.error("GraphQL INTERNAL_ERROR at {}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage(), ex);
        return buildError("Internal error", env, ErrorType.INTERNAL_ERROR, "INTERNAL_ERROR");
    }

    /**
     * Default catch-all for any other checked Exception.
     */
    @GraphQlExceptionHandler(Exception.class)
    public GraphQLError handleException(Exception ex,
                                        DataFetchingEnvironment env) {
        log.error("GraphQL INTERNAL_ERROR (checked) at {}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage(), ex);
        return buildError("Internal error", env, ErrorType.INTERNAL_ERROR, "INTERNAL_ERROR");
    }

    private GraphQLError buildError(String message,
                                    DataFetchingEnvironment env,
                                    ErrorType errorType,
                                    String errorCode) {
        return GraphqlErrorBuilder.newError()
                .message(message != null ? message : "Unexpected error")
                .errorType(errorType)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .extensions(Map.of(
                        "errorCode", errorCode
                ))
                .build();
    }
}
