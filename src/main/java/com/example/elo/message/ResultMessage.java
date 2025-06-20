package com.example.elo.message;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class ResultMessage extends BaseMessage {
    private List<String> images;
    private List<Object[]> result;

    public ResultMessage(List<String> images, List<Object[]> result){
        super("result");
        this.images=images;
        this.result=result;
    }
}
