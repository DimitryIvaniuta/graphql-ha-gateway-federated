package com.github.dimitryivaniuta.gateway.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple smoke test to ensure the GraphQL schema loads and basic query execution works.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class GraphQlSmokeTest extends BasePostgresTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void schemaLoadsAndEmptyQuerySucceeds() {
        GraphQlTester.Response response = graphQlTester
                .document("query { _empty }")
                .execute();

        response.errors().satisfy(errors -> assertThat(errors).isEmpty());
    }
}
