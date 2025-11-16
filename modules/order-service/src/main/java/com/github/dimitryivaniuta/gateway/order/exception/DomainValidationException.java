package com.github.dimitryivaniuta.gateway.order.exception;

/**
 * Thrown when domain invariants are violated.
 */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}