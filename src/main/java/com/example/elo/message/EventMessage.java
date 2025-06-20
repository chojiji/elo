package com.example.elo.message;

import lombok.Getter;

import java.util.List;

@Getter
public class EventMessage extends BaseMessage {
    private Boolean enter;
    private String nickName;
    private List<String> nickNames;


    public EventMessage(Boolean enter,String nickName,List<String> nickNames){
        super("event");
        this.enter=enter;
        this.nickName=nickName;
        this.nickNames=nickNames;
    }
}

