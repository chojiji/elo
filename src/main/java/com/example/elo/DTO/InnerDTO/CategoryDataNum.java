package com.example.elo.DTO.InnerDTO;

import lombok.Getter;

@Getter
public class CategoryDataNum {
    private String categoryName;
    private Integer dataNum;

    public CategoryDataNum(){

    }
    public CategoryDataNum(String categoryName, long dataNum) {
        this.categoryName = categoryName;
        this.dataNum = (int)dataNum;
    }
}
