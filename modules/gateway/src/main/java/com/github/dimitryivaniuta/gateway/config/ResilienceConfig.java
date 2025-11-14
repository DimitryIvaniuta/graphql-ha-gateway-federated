package com.github.dimitryivaniuta.gateway.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Baseline Resilience4j configuration for consistent policies across all outbound calls.
 *
 * <p>We expose registries so named instances can be created later in services/clis:
 * <ul>
 *   <li>{@code circuitBreakerRegistry.circuitBreaker("order")}</li>
 *   <li>{@code retryRegistry.retry("inventory")}</li>
 *   <li>{@code timeLimiterRegistry.timeLimiter("payment")}</li>
 *   <li>{@code threadPoolBulkheadRegistry.bulkhead("gateway")}</li>
 * </ul>
 *
 * <p>Defaults are conservative and production-safe; override per-client if needed.
 */
@Configuration
public class ResilienceConfig {

    // ---- Base configs ----

    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(50)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .failureRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slowCallRateThreshold(50.0f)
                .minimumNumberOfCalls(20)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(5)
                .recordExceptions(RuntimeException.class)
                .build();
    }

    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(200))
                .retryExceptions(RuntimeException.class)
                .build();
    }

    @Bean
    public TimeLimiterConfig defaultTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .cancelRunningFuture(true)
                .build();
    }

    @Bean
    public ThreadPoolBulkheadConfig defaultThreadPoolBulkheadConfig() {
        return ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(8)
                .maxThreadPoolSize(16)
                .queueCapacity(128)
                .build();
    }

    @Bean
    public BulkheadConfig defaultSemaphoreBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(20)
                .maxWaitDuration(Duration.ofMillis(0))
                .build();
    }

    // ---- Registries ----

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig config) {
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry(RetryConfig config) {
        return RetryRegistry.of(config);
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry(TimeLimiterConfig config) {
        return TimeLimiterRegistry.of(config);
    }

    @Bean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry(ThreadPoolBulkheadConfig config) {
        return ThreadPoolBulkheadRegistry.of(config);
    }
}
