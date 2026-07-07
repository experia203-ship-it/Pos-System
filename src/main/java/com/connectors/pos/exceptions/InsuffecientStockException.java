package com.connectors.pos.exceptions;

public class InsuffecientStockException extends RuntimeException {

public InsuffecientStockException(String message){

    super(message);
}
}
