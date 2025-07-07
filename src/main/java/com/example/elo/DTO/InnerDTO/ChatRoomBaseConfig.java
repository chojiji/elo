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
    private Integer maxUserNum;
    private Integer currentUserNum;
    private Integer time;

    public ChatRoomBaseConfig(String roomID, String roomName, String categoryName, Boolean isPassword,
                              Integer currentUserNum, Integer maxUserNum, Integer time) {
        this.roomID = roomID;
        this.roomName = roomName;
        this.categoryName = categoryName;
        this.isPassword = isPassword;
        this.currentUserNum = currentUserNum;
        this.maxUserNum = maxUserNum;
        this.time = time;
    }

}
