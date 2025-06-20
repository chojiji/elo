package com.example.elo.message;


import lombok.Getter;

@Getter
public class BaseMessage {
    private String type;
    private Long time;

    public BaseMessage(String type){
        this.type=type;
        this.time = System.currentTimeMillis();
    }
}
