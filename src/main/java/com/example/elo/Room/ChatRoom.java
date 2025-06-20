package com.example.elo.Room;

import com.example.elo.DTO.ChatRoomConfig;
import com.example.elo.DTO.VotePayload;
import com.example.elo.exception.IncorrectPasswordException;
import com.example.elo.exception.InvalidInputException;
import com.example.elo.exception.InvalidUserIDException;
import com.example.elo.exception.RoomFullException;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class ChatRoom {
    private String roomID;
    public String roomName;
    private String groupName;
    private String topic;
    private Map<String,String> userIDColor;
    private Deque<String> colors;
    private Deque<String> images;
    private Integer baseNumber;
    private Integer maxUser;
    private Integer timeLimit;
    private String password;
    private final AtomicInteger state; //0-초기,1-방공개,2-시작
    private Game game;
    private Set<String> banUsers;
    private final Lock lock = new ReentrantLock();
    public ChatRoom(ChatRoomConfig chatRoomConfig,String roomID,List<String> images,List<String> colors){
        this.roomID=roomID;
        this.roomName=chatRoomConfig.getRoomName();
        this.groupName=chatRoomConfig.getGroupName();
        this.topic= chatRoomConfig.getTopic();
        this.userIDColor =new LinkedHashMap<>();
        this.baseNumber = chatRoomConfig.getImageNum();
        this.maxUser=chatRoomConfig.getUserLimit();
        this.timeLimit=chatRoomConfig.getTimeLimit();
        this.password=chatRoomConfig.getPassword();
        this.images=new ArrayDeque<>(images);
        this.colors=new ArrayDeque<>(colors.subList(0,maxUser));
        this.state=new AtomicInteger(0);
        this.banUsers=new HashSet<>();
    }
    public void addUser(String userID){
        lock.lock();
        try{
            if(state.get()==0 ||state.get()==1){
                checkUser(userID);
                if(colors.size()==0){
                    throw new RoomFullException();
                }
                String color = colors.pollFirst();
                userIDColor.put(userID,color);
            }
        }finally {
            lock.unlock();
        }
    }

    public void removeUser(String userID) {
        lock.lock();
        try{
            String currentColor= userIDColor.remove(userID);
            if (currentColor==null){
                throw new InvalidUserIDException();
            }
            colors.offerLast(currentColor);
            if(userIDColor.size()==0){
                state.set(-1);
                if(this.game!=null){
                    this.game.shutdownGame();
                }
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void addBanList(String userID){
        banUsers.add(userID);
    }
    public void checkPassword(String passwordInput){
        if(!passwordInput.equals(password)){
            throw new IncorrectPasswordException();
        }
    }
    public void checkBanList(String userID){
        if(banUsers.contains(userID)){
            throw new RoomFullException();
        }
    }
    public void checkUser(String userID){
        if(userIDColor.containsKey(userID)){
            throw new InvalidInputException("이미 해당 채팅방에 존재합니다.");
        }
    }
    public void ensureUserExist(String userID){
        if(!(userIDColor.containsKey(userID))){
            throw new InvalidInputException("해당 채팅방에 유저가 존재하지 않습니다.");
        }
    }


    public void checkUserLimit(){
        if (this.maxUser==getUserNumber()){
            throw new RoomFullException();
        }
    }


    public String getColor(String sessionID) {
        String color = userIDColor.get(sessionID);
        if (color == null) {
            throw new InvalidUserIDException();
        }
        return color;
    }
    public boolean isHost(String userID){
        Iterator<String> iterator = userIDColor.keySet().iterator();
        if (iterator.hasNext()) {
            String hostUserID = iterator.next();
            return userID.equals(hostUserID);
        }
        return false;
    }

    public void startGame(Game game){
        lock.lock();
        try{
            if(state.compareAndSet(1,2)){
                this.game=game;
                game.runGame(0,0,true);
            }
        }finally {
            lock.unlock();
        }
    }
    public void vote(String userID, boolean voteLeft, int round){
        lock.lock();
        try{
            if(state.get()==2){
                this.game.vote(userID,voteLeft,round);
            }
        }finally {
            lock.unlock();
        }
    }


    public List<String> getNickNames(){
        return new ArrayList<>(userIDColor.values());
    }
    public String getNickName(String sessionID){return userIDColor.get(sessionID);}

    public Integer getUserNumber(){
        return userIDColor.size();
    }

}

