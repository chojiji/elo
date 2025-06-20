package com.example.elo.DTO.InnerDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomBaseConfig {
    private String roomName;
    private String categoryName;
    private Boolean isPassword;
    private String roomID;
    private Integer MaxUserNum;
    private Integer currentUserNum;
    private Integer time;

    public ChatRoomBaseConfig(){
    }
}
