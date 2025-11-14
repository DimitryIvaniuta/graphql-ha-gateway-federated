package com.github.dimitryivaniuta.gateway.util;

import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GraphQL {@code DateTime} scalar.
 *
 * <p>Represents instants as ISO-8601 {@link OffsetDateTime} strings, for example:
 * {@code 2025-11-14T10:15:30+00:00}.</p>
 *
 * <p>Java type: {@link OffsetDateTime}, GraphQL type: {@code String}.</p>
 */
public final class DateTimeScalar {

    /**
     * Singleton instance used in {@link com.github.dimitryivaniuta.gateway.config.GraphQlConfig}.
     */
    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("ISO-8601 DateTime with offset, e.g. 2025-11-14T10:15:30+00:00")
            .coercing(new Coercing<OffsetDateTime, String>() {

                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult == null) {
                        return null;
                    }
                    if (dataFetcherResult instanceof OffsetDateTime odt) {
                        return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    }
                    throw new CoercingSerializeException(
                            "Expected an OffsetDateTime object but was: " + dataFetcherResult.getClass().getName());
                }

                @Override
                public OffsetDateTime parseValue(Object input) throws CoercingParseValueException {
                    if (input == null) {
                        return null;
                    }
                    if (input instanceof String s) {
                        try {
                            return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        } catch (Exception ex) {
                            throw new CoercingParseValueException("Invalid DateTime value: " + s, ex);
                        }
                    }
                    throw new CoercingParseValueException(
                            "Expected a String for DateTime but was: " + input.getClass().getName());
                }

                @Override
                public OffsetDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue stringValue) {
                        try {
                            return OffsetDateTime.parse(stringValue.getValue(),
                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        } catch (Exception ex) {
                            throw new CoercingParseLiteralException("Invalid DateTime literal: " + stringValue.getValue(), ex);
                        }
                    }
                    throw new CoercingParseLiteralException(
                            "Expected a StringValue AST node for DateTime literal but was: " +
                                    (input == null ? "null" : input.getClass().getName()));
                }
            })
            .build();

    private DateTimeScalar() {
        // utility, not meant to be instantiated
    }
}
