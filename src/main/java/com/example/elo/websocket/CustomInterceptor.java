package com.example.elo.websocket;

import com.example.elo.Room.ChatRoomManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class CustomInterceptor implements ChannelInterceptor {

    private final ChatRoomManager chatRoomManager;

    @Autowired
    public CustomInterceptor(@Lazy ChatRoomManager chatRoomManager){
        this.chatRoomManager=chatRoomManager;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && accessor.getCommand() != null) {
            if (accessor.getCommand().equals(StompCommand.SUBSCRIBE)) {
                subscribeHandler(accessor);
            }
            else if (accessor.getCommand().equals(StompCommand.CONNECT)) {
                connectHandler(accessor);
            }
        }

        return message;
    }


    private void subscribeHandler(StompHeaderAccessor accessor){
        String destination = accessor.getDestination();
        try{
            if (destination != null) {
                if(destination.matches("^/chat/.*$")){
                    String userID = accessor.getUser().getName();
                    String[] parts = destination.split("/");
                    String roomID = parts[parts.length - 1];
                    String password = accessor.getFirstNativeHeader("password");
                    chatRoomManager.checkUserValidation(roomID,password,userID);
                    chatRoomManager.addUser(roomID, userID);
                }
                else if(destination.matches("^/user/queue/.*$")){
                    String[] parts = destination.split("/");
                    String userID = accessor.getUser().getName();
                    String roomID = parts[3];
                    chatRoomManager.getRoom(roomID).ensureUserExist(userID);
                }
                else{
                    throw new IllegalArgumentException("잘못된 주소입니다.");
                }
            }
            else{
                throw new IllegalArgumentException("잘못된 주소입니다.");
            }
        }catch (Exception ex) {
            throw new MessagingException(ex.getMessage());
        }
    }

    private void connectHandler(StompHeaderAccessor accessor) {
        String roomID = accessor.getFirstNativeHeader("roomID");
        String password = accessor.getFirstNativeHeader("password");
        try {
            if(roomID==null || password==null){
                throw new IllegalArgumentException("잘못된 요청입니다.");
            }
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                String httpSessionID = (String) sessionAttributes.get("httpSessionID");
                accessor.setUser(new CustomPrincipal(httpSessionID));
                chatRoomManager.checkUserValidation(roomID,password,httpSessionID);
            } else {
                throw new IllegalArgumentException("세션 속성이 존재하지 않습니다.");
            }
        } catch (Exception ex) {
            throw new MessagingException(ex.getMessage());
        }
    }

}
