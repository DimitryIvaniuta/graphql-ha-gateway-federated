package com.github.dimitryivaniuta.gateway.config;

import com.github.dimitryivaniuta.gateway.util.DateTimeScalar;
import com.github.dimitryivaniuta.gateway.util.MoneyScalar;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoaderRegistry;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * GraphQL runtime configuration (Spring GraphQL 1.3.x).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Register custom scalars declared in the SDL (DateTime, Money).</li>
 *   <li>Provide a baseline {@link DataLoaderRegistry} so batching can be added later.</li>
 *   <li>Convert exceptions thrown in data fetchers into structured GraphQL errors.</li>
 * </ul>
 *
 * <p>Spring Boot auto-detects {@link RuntimeWiringConfigurer}, {@link DataLoaderRegistry} and
 * {@link DataFetcherExceptionResolver} beans and applies them to the auto-built {@code GraphQlSource}.</p>
 */
@Configuration
@Slf4j
public class GraphQlConfig {

    /**
     * Wires custom scalars and any future type/directive wiring.
     *
     * <p>This method is called once when GraphQL Java builds the runtime schema.</p>
     * <ul>
     *   <li>Registers the {@code DateTime} scalar (maps to {@link java.time.OffsetDateTime}).</li>
     *   <li>Registers the {@code Money} scalar (maps to {@link java.math.BigDecimal}).</li>
     *   <li>Additional directives/type-resolvers can be added here later.</li>
     * </ul>
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                // Custom scalars used by the shared SDL (common-graphql module)
                .scalar(DateTimeScalar.INSTANCE)
                .scalar(MoneyScalar.INSTANCE)
                .scalar(ExtendedScalars.GraphQLBigDecimal);
        // Example hooks for the future:
        // .directive("auth", new AuthDirectiveWiring())
        // .type("Query", typeWiring -> typeWiring.dataFetcher("health", env -> "OK"));
    }

    /**
     * Baseline {@link DataLoaderRegistry}. You can register batch loaders later and
     * add them to this registry; Spring GraphQL will create a fresh instance per request.
     */
    @Bean
    public DataLoaderRegistry dataLoaderRegistry() {
        return DataLoaderRegistry.newRegistry().build();
    }

    /**
     * Maps exceptions from data fetchers into client-visible GraphQL errors, while logging
     * full details server-side.
     */
    @Bean
    public DataFetcherExceptionResolver dataFetcherExceptionResolver() {
        return new DataFetcherExceptionResolverAdapter() {
            @Override
            protected GraphQLError resolveToSingleError(@NotNull Throwable ex, @NotNull DataFetchingEnvironment env) {
                log.error("GraphQL error at path {} on field '{}': {}",
                        env.getExecutionStepInfo().getPath(),
                        env.getField().getName(),
                        ex.toString(),
                        ex);

                return GraphqlErrorBuilder.newError(env)
                        .message("Internal error while resolving field '%s'.".formatted(env.getField().getName()))
                        .extensions(Map.of(
                                "timestamp", OffsetDateTime.now().toString(),
                                "errorType", ex.getClass().getSimpleName()
                        ))
                        .build();
            }
        };
    }
}
