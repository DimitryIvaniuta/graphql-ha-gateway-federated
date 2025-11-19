package com.github.dimitryivaniuta.gateway.config;

import com.github.dimitryivaniuta.gateway.config.properties.OrderServiceProperties;
import com.github.dimitryivaniuta.gateway.config.properties.SecurityProperties;
import com.github.dimitryivaniuta.gateway.config.properties.CorsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        SecurityProperties.class,
        CorsProperties.class,
        OrderServiceProperties.class
})
public class PropertiesConfig {
    // no beans needed – just a registration point
}
