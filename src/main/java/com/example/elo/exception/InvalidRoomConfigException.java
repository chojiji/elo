package com.example.elo.exception;

public class InvalidRoomConfigException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "유효하지 않은 설정입니다.";

    public InvalidRoomConfigException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidRoomConfigException(String message) {
        super(message);
    }
}