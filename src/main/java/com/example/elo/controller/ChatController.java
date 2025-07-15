package com.example.elo.controller;

import com.example.elo.DTO.VotePayload;
import com.example.elo.exception.InvalidRoomConfigException;
import com.example.elo.message.BaseMessage;
import com.example.elo.message.ChatMessage;
import com.example.elo.message.EventMessage;
import com.example.elo.Room.ChatRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
public class ChatController {

    private ChatRoomManager chatRoomManager;

    @Autowired
    public ChatController(ChatRoomManager chatRoomManager){
        this.chatRoomManager=chatRoomManager;
    }

    @MessageMapping("/{roomID}/chat")
    @SendTo("/chat/{roomID}")
    public BaseMessage handleChatMessage(@DestinationVariable String roomID, String message, Principal principal) {
        String userID = principal.getName();
        String nickname=chatRoomManager.getUserColor(roomID,userID);
        BaseMessage chatMessage = new ChatMessage(nickname,message);
        return chatMessage;
    }
    @MessageMapping("/{roomID}/vote")
    @SendTo("/chat/{roomID}")
    public void handleVote(@DestinationVariable String roomID, @Payload VotePayload votePayload, Principal principal) {
        String userID = principal.getName();
        chatRoomManager.vote(userID,roomID,votePayload);

    }



    @SubscribeMapping("/chat/{roomID}")
    @SendTo("/chat/{roomID}")
    public BaseMessage joinRoom(@DestinationVariable String roomID, @Header("simpUser") Principal principal){
        String userID = principal.getName();
        String nickName=chatRoomManager.getUserColor(roomID,userID);
        List<String> nickNames = chatRoomManager.getNickNames(roomID);
        BaseMessage basemessage = new EventMessage(true,nickName,nickNames);
        return basemessage;
    }

    @SubscribeMapping("/queue/{roomID}/{userID}")
    @SendToUser("/queue/{roomID}/{userID}")
    public String joinRoomPersonal(@DestinationVariable String roomID, @DestinationVariable String userID){
        String nickName=chatRoomManager.getUserColor(roomID,userID);
        return nickName;
    }

    @MessageMapping("/{roomID}/startGame")
    public void startGame(@DestinationVariable String roomID,Principal principal){
        String userID = principal.getName();
        chatRoomManager.startGame(roomID,userID);
    }

    @MessageExceptionHandler(InvalidRoomConfigException.class)
    @SendToUser("/queue/errors")
    public String handleInvalidRoomConfigException(InvalidRoomConfigException ex) {
        return ex.getMessage();
    }
}
