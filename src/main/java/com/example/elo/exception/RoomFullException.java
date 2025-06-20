package com.example.elo.exception;

public class RoomFullException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "방이 꽉 찼습니다.";

    public RoomFullException() {
        super(DEFAULT_MESSAGE);
    }

    public RoomFullException(String message) {
        super(message);
    }
}
