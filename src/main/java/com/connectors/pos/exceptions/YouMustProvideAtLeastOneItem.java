package com.connectors.pos.exceptions;

public class YouMustProvideAtLeastOneItem extends RuntimeException {
    public YouMustProvideAtLeastOneItem(String message) {
        super(message);
    }
}
