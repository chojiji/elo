package com.example.elo.message;

import lombok.Getter;

@Getter
public class ConfigMessage extends BaseMessage {
    private String nickName;

    public ConfigMessage(String nickName){
        super("config");
        this.nickName=nickName;
    }
}
