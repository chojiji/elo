package com.example.elo.controller;

import com.example.elo.exception.InvalidRoomConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

@ControllerAdvice
public class SocketException {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(InvalidRoomConfigException.class)
    public void handleInvalidRoomConfigException(InvalidRoomConfigException ex, Principal principal) {
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                ex.getMessage()
        );
    }
}
