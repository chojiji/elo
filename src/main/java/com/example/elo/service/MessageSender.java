package com.example.elo.service;

import com.example.elo.message.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MessageSender {
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public MessageSender(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void configSend(String sessionID,String roomID,String nickName){
        BaseMessage message=new ConfigMessage(nickName);
        simpMessagingTemplate.convertAndSendToUser(sessionID,"chat/"+roomID,message);
    }

    public void authSend(String sessionID,String roomID){
        BaseMessage message= new BaseMessage("auth");
        simpMessagingTemplate.convertAndSendToUser(sessionID,"chat/"+roomID,message);
    }
    public void broadcastGameUpdate(String roomID, int currentRound, int left, int right, boolean leftWon,
                                    String firstImageURL, String secondImageURL, Long timeRemainingMillis) {
        ImageVoteMessage payload = new ImageVoteMessage(
                currentRound,
                left,
                right,
                leftWon,
                firstImageURL,
                secondImageURL,
                timeRemainingMillis
        );
        String destination = "/chat/" + roomID;
        simpMessagingTemplate.convertAndSend(destination, payload);
    }
    public void broadcastResult(String roomID, Deque<String> images,List<Object[]> result){
        String destination = "/chat/" + roomID;
        ResultMessage resultMessage = new ResultMessage(new ArrayList<>(images), result);
        simpMessagingTemplate.convertAndSend(destination, resultMessage);
    }
}
