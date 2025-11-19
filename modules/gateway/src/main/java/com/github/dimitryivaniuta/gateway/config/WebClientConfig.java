package com.github.dimitryivaniuta.gateway.config;

//import io.netty.channel.ChannelOption;
//import io.netty.handler.timeout.ReadTimeoutHandler;
//import io.netty.handler.timeout.WriteTimeoutHandler;
import com.github.dimitryivaniuta.gateway.config.properties.OrderServiceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;

import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.*;

/**
 * Central configuration for reactive {@link WebClient} instances used by the gateway
 * to call downstream domain services (order, inventory, payment).
 *
 * <p>Design goals:
 * <ul>
 *   <li>Single, shared {@link WebClient.Builder} with consistent timeouts and buffer limits.</li>
 *   <li>Service-specific clients configured via strongly-typed properties.</li>
 *   <li>Safe defaults that work for production (connect/read timeouts, reasonable buffer limit).</li>
 * </ul>
 *
 * <p>Configuration properties (from {@code application.yml}):
 * <pre>
 * services:
 *   order:
 *     base-url: http://localhost:8081
 *   inventory:
 *     base-url: http://localhost:8082
 *   payment:
 *     base-url: http://localhost:8083
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(WebClientConfig.ServicesProperties.class)
public class WebClientConfig {

    /**
     * Shared {@link ExchangeStrategies} with increased max in-memory buffer to handle
     * larger GraphQL / JSON payloads safely (10 MiB by default).
     */
    @Bean
    public ExchangeStrategies webClientExchangeStrategies() {
        int maxBytes = (int) DataSize.ofMegabytes(10).toBytes(); // 10 MiB
        return ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(maxBytes))
                .build();
    }

    /**
     * Reactor Netty {@link ClientHttpConnector} with sane timeouts.
     *
     * <ul>
     *   <li>Connect timeout: 5 seconds</li>
     *   <li>Read timeout: 10 seconds</li>
     *   <li>Write timeout: 10 seconds</li>
     * </ul>
     */
//    @Bean
//    public ClientHttpConnector webClientHttpConnector() {
//        HttpClient httpClient = HttpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
//                .responseTimeout(Duration.ofSeconds(10))
//                .doOnConnected(conn -> conn
//                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
//                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));
//        return new ReactorClientHttpConnector(httpClient);
//    }

    /**
     * Base {@link WebClient.Builder} used by all downstream clients.
     * <p>
     * By centralizing the connector and strategies here we ensure consistent behavior
     * (timeouts, buffer limits, default headers) across all outbound calls.
     */
    @Bean
    public WebClient.Builder webClientBuilder(ClientHttpConnector webClientHttpConnector,
                                              ExchangeStrategies webClientExchangeStrategies) {
        return WebClient.builder()
                .clientConnector(webClientHttpConnector)
                .exchangeStrategies(webClientExchangeStrategies)
                .defaultHeader("Accept", "application/json");
    }

    // -------------------------------------------------------------------------
    // Service-specific WebClient instances
    // -------------------------------------------------------------------------

    /**
     * WebClient for the Order service.
     */
    @Bean(name = "orderWebClient")
    public WebClient orderWebClient(WebClient.Builder builder, ServicesProperties servicesProperties) {
        return builder.baseUrl(servicesProperties.order().baseUrl()).build();
    }

    /**
     * WebClient for the Inventory service.
     */
    @Bean(name = "inventoryWebClient")
    public WebClient inventoryWebClient(WebClient.Builder builder, ServicesProperties servicesProperties) {
        return builder.baseUrl(servicesProperties.inventory().baseUrl()).build();
    }

    /**
     * WebClient for the Payment service.
     */
    @Bean(name = "paymentWebClient")
    public WebClient paymentWebClient(WebClient.Builder builder, ServicesProperties servicesProperties) {
        return builder.baseUrl(servicesProperties.payment().baseUrl()).build();
    }

    // -------------------------------------------------------------------------
    // Strongly-typed configuration properties
    // -------------------------------------------------------------------------

    /**
     * Properties for downstream service endpoints.
     *
     * <p>Bound from {@code services.*} prefix in {@code application.yml}.</p>
     */
    @ConfigurationProperties(prefix = "services")
    public record ServicesProperties(Service order, Service inventory, Service payment) {

        /**
         * Single service endpoint configuration.
         *
         * @param baseUrl base URL for the service, e.g. {@code http://localhost:8081}
         */
        public record Service(String baseUrl) { }
    }


    @Bean
    public ExchangeFilterFunction jwtPropagationFilter() {
        return (request, next) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String tokenValue = jwtAuth.getToken().getTokenValue();

                ClientRequest mutated = ClientRequest.from(request)
                        .headers(headers -> headers.setBearerAuth(tokenValue))
                        .build();

                return next.exchange(mutated);
            }

            return next.exchange(request);
        };
    }

    @Bean
    public WebClient orderServiceWebClient(WebClient.Builder builder,
                                           ExchangeFilterFunction jwtPropagationFilter,
                                           OrderServiceProperties orderServiceProperties) {
        return builder
                .baseUrl(orderServiceProperties.baseUrl())
                .filter(jwtPropagationFilter)
                .build();
    }

}
