package com.example.elo.DTO;

import com.example.elo.DTO.InnerDTO.CategoryDataNum;
import com.example.elo.DTO.InnerDTO.ChatRoomBaseConfig;
import lombok.Getter;

import java.util.List;

@Getter
public class GroupChatInfo {
    private List<ChatRoomBaseConfig> chatRoomBaseConfigList;
    private List<CategoryDataNum> categoryDataNumList;

    public GroupChatInfo(){
    }
    public GroupChatInfo(List<ChatRoomBaseConfig> chatRoomBaseConfigList,List<CategoryDataNum> categoryDataNum){
        this. chatRoomBaseConfigList = chatRoomBaseConfigList;
        this.categoryDataNumList = categoryDataNum;
    }
}
