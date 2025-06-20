package com.example.elo.DTO;

import lombok.Getter;

@Getter
public class GroupInfo {
    private final String groupName;
    private final String description;
    private String mainImageURL;

    public GroupInfo(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }
    public void setMainImage(String imageURL){
        this.mainImageURL=imageURL;
    }
}
