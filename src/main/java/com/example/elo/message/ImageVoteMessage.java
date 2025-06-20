package com.example.elo.message;

import lombok.Getter;

@Getter
public class ImageVoteMessage extends BaseMessage{
    private int currentRound;
    private int left;
    private int right;
    private boolean leftWin;
    private String firstImageURL;
    private String secondImageURL;
    private Long timeRemainingMillis;

    public ImageVoteMessage(int currentRound, int left, int right, boolean leftWin, String firstImageURL, String secondImageURL, Long timeRemainingMillis) {
        super("Image");
        this.currentRound=currentRound;
        this.left = left;
        this.right = right;
        this.leftWin = leftWin;
        this.firstImageURL = firstImageURL;
        this.secondImageURL = secondImageURL;
        this.timeRemainingMillis = timeRemainingMillis;
    }

}

