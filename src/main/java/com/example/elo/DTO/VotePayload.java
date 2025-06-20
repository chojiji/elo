package com.example.elo.DTO;

public class VotePayload {
    private boolean voteForFirst;
    private int round;

    // 기본 생성자 (Jackson 역직렬화를 위해 필요)
    public VotePayload() {
    }

    public VotePayload(boolean voteForFirst, int round) {
        this.voteForFirst = voteForFirst;
        this.round = round;
    }

    // Getters and Setters

    public boolean isVoteForFirst() {
        return voteForFirst;
    }

    public void setVoteForFirst(boolean voteForFirst) {
        this.voteForFirst = voteForFirst;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    @Override
    public String toString() {
        return "VotePayload{" +
                "voteForFirst=" + voteForFirst +
                ", round=" + round +
                '}';
    }
}