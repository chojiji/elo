package com.example.elo.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ChatRoomConfig {
    private String groupName;
    private String roomName;
    private String topic;
    private String password;
    private Integer timeLimit;
    private Integer userLimit;
    private Integer imageNum;
}
