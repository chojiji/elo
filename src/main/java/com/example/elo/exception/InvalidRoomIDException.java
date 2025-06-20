package com.example.elo.exception;

public class InvalidRoomIDException extends  RuntimeException{
    private static final String DEFAULT_MESSAGE = "해당 채팅방은 존재하지 않습니다.";

    public InvalidRoomIDException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidRoomIDException(String message) {
        super(message);
    }
}
