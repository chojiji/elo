package com.example.elo.exception;

public class IncorrectPasswordException extends RuntimeException{
    private static final String DEFAULT_MESSAGE = "비밀번호가 올바르지 않습니다.";

    public IncorrectPasswordException() {
        super(DEFAULT_MESSAGE);
    }

    public IncorrectPasswordException(String message) {
        super(message);
    }
}