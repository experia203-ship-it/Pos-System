package com.connectors.pos.exceptions;

public class OrderNotFoundException extends RuntimeException{


    public OrderNotFoundException(String message){

        super(message);
    }
}
