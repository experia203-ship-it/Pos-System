package com.connectors.pos.exceptions;

public class ProductNotFoundException extends RuntimeException{

    public ProductNotFoundException(String message){

        super(message);
    }
}
