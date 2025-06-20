package com.example.elo.exception;

public class InvalidInputException extends RuntimeException{
    private static final String DEFAULT_MESSAGE = "잘못된 입력입니다.";

    public InvalidInputException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidInputException(String message) {
        super(message);
    }
}
