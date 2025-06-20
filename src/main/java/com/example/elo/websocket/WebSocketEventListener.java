package com.example.elo.websocket;

import com.example.elo.Room.ChatRoomManager;
import com.example.elo.message.BaseMessage;
import com.example.elo.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.List;

@Component
public class WebSocketEventListener {

    private ChatRoomManager chatRoomManager;
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public WebSocketEventListener(ChatRoomManager chatRoomManager, SimpMessagingTemplate simpMessagingTemplate){
        this.chatRoomManager=chatRoomManager;
        this.simpMessagingTemplate=simpMessagingTemplate;
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();

        if (destination != null && destination.startsWith("/chat/")) {
            String roomID = destination.split("/")[1];
            String userID = event.getUser().getName();

            String nickName = chatRoomManager.getUserColor(roomID, userID);
            List<String> nickNames = chatRoomManager.getNickNames(roomID);

            BaseMessage baseMessage = new EventMessage(true, nickName, nickNames);

            simpMessagingTemplate.convertAndSend(destination, baseMessage);
        }
        else if(destination != null && destination.startsWith("/queue/")){
            String[] destinationList = destination.split("/");
            String roomID = destinationList[1];
            String userID = destinationList[2];
        }
    }
}
