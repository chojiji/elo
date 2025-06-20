package com.example.elo.DTO;

import lombok.Getter;

@Getter
public class EloData {
    private  String groupName;
    private  String name;
    private  Integer elo;
    private  String thumbnailUrl;

    public EloData(){
    }

    public EloData(String groupName, String name, Integer elo) {
        this.groupName=groupName;
        this.elo=elo;
        this.name=name;
        this.thumbnailUrl="thumbnails/"+groupName+"/"+name+".png";
    }
}
