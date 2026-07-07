package com.connectors.pos.exceptions;

public class UserNotFoundException extends RuntimeException {



    public UserNotFoundException(String message){

        super(message);

    }
}
