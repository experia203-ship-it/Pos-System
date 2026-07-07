package com.connectors.pos.exceptions;

public class UserNameAlreadyExistsException extends RuntimeException{


    public UserNameAlreadyExistsException(String message){

        super(message);
    }
}
