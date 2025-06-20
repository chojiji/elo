package com.example.elo.exception;

public class InvalidUserIDException extends RuntimeException{
    private static final String DEFAULT_MESSAGE = "유저는 해당 채팅방에 존재하지 않습니다.";

    public InvalidUserIDException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidUserIDException(String message) {
        super(message);
    }
}
