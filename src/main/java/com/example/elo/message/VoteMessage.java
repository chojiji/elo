package com.example.elo.message;

import lombok.Getter;

@Getter
public class VoteMessage extends BaseMessage {
    private String nickName;

    public VoteMessage(String nickName){
        super("vote");
        this.nickName=nickName;
    }
}
