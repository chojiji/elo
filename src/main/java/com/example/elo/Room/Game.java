package com.example.elo.Room;

import com.example.elo.entity.CategoryDataRelation;
import com.example.elo.service.EloGetService;
import com.example.elo.service.MessageSender;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Game {
    private int currentRound;
    private String groupName;
    private String categoryName;
    private Map<String, Boolean> votes;
    private String roomID;
    private final Deque<String> images;
    private final Deque<String> trash;
    private final Map<String, String> userIDColor;
    private final int timeLimitPerRound;
    private final EloGetService eloGetService;
    private final MessageSender messageSender;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> currentTimerTask;

    public Game(String roomID, Deque<String> images, Map<String, String> userIDColor, int timeLimitPerRound, String groupName, String categoryName, EloGetService eloGetService, MessageSender messageSender) {
        this.currentRound = 0;
        this.roomID=roomID;
        this.votes = new LinkedHashMap<>();
        this.images = images;
        this.userIDColor = userIDColor;
        this.timeLimitPerRound = timeLimitPerRound+3;
        this.eloGetService=eloGetService;
        this.messageSender=messageSender;
        this.groupName=groupName;
        this.categoryName=categoryName;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        trash=new ArrayDeque<>();
    }

    public void runGame(int left, int right, boolean leftWin) {
        currentRound++;
        currentTimerTask = scheduler.schedule(() -> {
            openVote();
        }, timeLimitPerRound, TimeUnit.SECONDS);
        broadCastImage(left,right,leftWin,getFirstImage(),getSecondImage(),currentTimerTask.getDelay(TimeUnit.MILLISECONDS));
    }
    public void broadCastImage(int left,int right, boolean leftWin, String firstImage,String secondImage,Long time){
        String firstImageURL = groupName+"/"+firstImage+".png";
        String secondImageURL = groupName+"/"+secondImage+".png";
        messageSender.broadcastGameUpdate(this.roomID,currentRound, left, right, leftWin,
                firstImageURL, secondImageURL, time);
    }

    public void endGame(int left, int right, boolean leftWin){
        long temp = 1000;
        broadCastImage(left,right,leftWin,getFirstImage(),getFirstImage(),temp);
        trash.add(images.pop());
        List<Object[]> result = eloGetService.getResult(groupName,categoryName);
        messageSender.broadcastResult(roomID,trash,result);

    }

    private void openVote() {
        int left=0;
        int right=0;
        boolean leftWin=true;
        eloGetService.calculateEloAndUpdate(groupName,categoryName,getFirstImage(),getSecondImage(),this.votes.values());
        for (Boolean values : this.votes.values()) {
            if(values){
                left++;
            }
            else{
                right++;
            }
        }
        if(left==right){
            Iterator<String> iterator = userIDColor.keySet().iterator();
            while(iterator.hasNext()){
                String hostUserID = iterator.next();
                if(votes.containsKey(hostUserID)){
                    if(!votes.get(hostUserID)){
                        leftWin=false;
                    }
                    break;
                }
            }
        }
        if(left>=right&&leftWin){
            images.addLast(images.removeFirst());
            trash.add(images.removeFirst());
        }
        else{
            leftWin=false;
            trash.add(images.removeFirst());
            images.addLast(images.removeFirst());
        }

        if(images.size()==1){
            endGame(left,right,leftWin);
        }
        else{
            votes.clear();
            runGame(left,right,leftWin);
        }
    }
    public void shutdownGame() {
        if (currentTimerTask != null && !currentTimerTask.isDone()) {
            boolean cancelled = currentTimerTask.cancel(true); // true: 실행 중인 태스크 인터럽트 시도
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown(); // 새로운 작업 제출 거부, 기존 작업 완료 시도
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) { // 지정된 시간 동안 종료 대기
                    scheduler.shutdownNow(); // 강제 종료
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt(); // 현재 스레드의 인터럽트 상태 복원
            }
        }
    }

    public void vote(String userID, boolean voteLeft, int round) {
        if(userIDColor.containsKey(userID) &&round==this.currentRound && currentTimerTask!=null &&!currentTimerTask.isDone()){
            if(!votes.containsKey(userID)){
                votes.put(userID,voteLeft);
                if(votes.size()== userIDColor.size()) {
                    if(currentTimerTask.cancel(false)){
                        openVote();
                    }
                }
            }
        }
    }
    private String getFirstImage(){
        return images.peekFirst();
    }
    private String getSecondImage() {
        Iterator<String> iterator = images.iterator();
        iterator.next();
        return iterator.next();
    }

}
