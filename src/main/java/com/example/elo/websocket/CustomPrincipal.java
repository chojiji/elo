package com.example.elo.websocket;


import java.security.Principal;

public class CustomPrincipal implements Principal {
    private final String sessionId;

    public CustomPrincipal(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getName() {
        return sessionId;
    }
}
