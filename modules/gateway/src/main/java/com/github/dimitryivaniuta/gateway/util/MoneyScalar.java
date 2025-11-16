package com.github.dimitryivaniuta.gateway.util;

import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.math.BigDecimal;

/**
 * GraphQL {@code Money} scalar.
 *
 * <p>Represents a monetary amount as a decimal number without currency information.
 * Typical usage is to pair this scalar with a separate {@code currency} field.</p>
 *
 * <p>Java type: {@link BigDecimal}, GraphQL type: {@code String} (for consistent JSON representation).</p>
 */
public final class MoneyScalar {

    /**
     * Singleton instance used in {@link com.github.dimitryivaniuta.gateway.config.GraphQlConfig}.
     */
    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("Money")
            .description("Monetary amount represented as decimal string (currency handled separately)")
            .coercing(new Coercing<BigDecimal, String>() {

                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult == null) {
                        return null;
                    }
                    if (dataFetcherResult instanceof BigDecimal bd) {
                        return bd.toPlainString();
                    }
                    if (dataFetcherResult instanceof Number number) {
                        return BigDecimal.valueOf(number.doubleValue()).toPlainString();
                    }
                    if (dataFetcherResult instanceof String s) {
                        try {
                            return new BigDecimal(s).toPlainString();
                        } catch (NumberFormatException ex) {
                            throw new CoercingSerializeException("Invalid Money value: " + s, ex);
                        }
                    }
                    throw new CoercingSerializeException(
                            "Expected BigDecimal/Number/String for Money but was: " + dataFetcherResult.getClass().getName());
                }

                @Override
                public BigDecimal parseValue(Object input) throws CoercingParseValueException {
                    if (input == null) {
                        return null;
                    }
                    if (input instanceof BigDecimal bd) {
                        return bd;
                    }
                    if (input instanceof Number number) {
                        return BigDecimal.valueOf(number.doubleValue());
                    }
                    if (input instanceof String s) {
                        try {
                            return new BigDecimal(s);
                        } catch (NumberFormatException ex) {
                            throw new CoercingParseValueException("Invalid Money value: " + s, ex);
                        }
                    }
                    throw new CoercingParseValueException(
                            "Expected BigDecimal/Number/String for Money but was: " + input.getClass().getName());
                }

                @Override
                public BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof IntValue intValue) {
                        return new BigDecimal(intValue.getValue());
                    }
                    if (input instanceof FloatValue floatValue) {
                        return floatValue.getValue();
                    }
                    if (input instanceof StringValue stringValue) {
                        try {
                            return new BigDecimal(stringValue.getValue());
                        } catch (NumberFormatException ex) {
                            throw new CoercingParseLiteralException("Invalid Money literal: " + stringValue.getValue(), ex);
                        }
                    }
                    throw new CoercingParseLiteralException(
                            "Expected IntValue/FloatValue/StringValue for Money literal but was: " +
                                    (input == null ? "null" : input.getClass().getName()));
                }
            })
            .build();

    private MoneyScalar() {
        // utility, not meant to be instantiated
    }
}
