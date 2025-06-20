package com.example.elo.message;

import lombok.Getter;

@Getter
public class ChatMessage extends BaseMessage {
    private String nickName;
    private String content;

    public ChatMessage(String nickName, String content){
        super("chat");
        this.nickName=nickName;
        this.content=content;
    }
}
