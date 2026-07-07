package com.connectors.pos.exceptions;

public class CustomerMustBeProvidedException extends RuntimeException {
    public CustomerMustBeProvidedException(String message) {

        super(message);
    }
}
