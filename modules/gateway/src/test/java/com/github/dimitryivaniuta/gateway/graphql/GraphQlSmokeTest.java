package com.github.dimitryivaniuta.gateway.graphql;

import com.github.dimitryivaniuta.gateway.GatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("test")
class GraphQlSmokeTest {

    @Autowired
    HttpGraphQlTester graphQlTester;

    @Test
    void schemaLoaded_andGraphQlEndpointAvailable() {
        graphQlTester
                .document("query { __schema { queryType { name } } }")
                .execute()
                .path("__schema.queryType.name")
                .entity(String.class)
                .isEqualTo("Query");
    }
}
