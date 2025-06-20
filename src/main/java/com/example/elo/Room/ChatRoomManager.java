package com.example.elo.Room;

import com.example.elo.DTO.ChatRoomConfig;
import com.example.elo.DTO.InnerDTO.ChatRoomBaseConfig;
import com.example.elo.DTO.VotePayload;
import com.example.elo.exception.InvalidInputException;
import com.example.elo.exception.InvalidRoomConfigException;
import com.example.elo.exception.RoomFullException;
import com.example.elo.service.DataService;
import com.example.elo.service.EloGetService;
import com.example.elo.service.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ChatRoomManager {

    private final Map<String, ChatRoom> chatRoomIDMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<ChatRoom>>> chatRoomGroupNameMap = new ConcurrentHashMap<>();
    private final DataService dataService;
    private final EloGetService eloGetService;
    private final MessageSender messageSender;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    //region
    @Value("${chatConfig.maxUser}")
    private Integer maxUser;

    @Value("${chatConfig.maxImage}")
    private Integer maxImage;

    @Value("${chatConfig.minImage}")
    private Integer minImage;

    @Value("${chatConfig.maxTime}")
    private Integer maxTime;

    @Value("${chatConfig.minTime}")
    private Integer minTime;

    @Value("${colors}")
    private List<String> colors;
    //endregion
    @Autowired
    public ChatRoomManager(DataService dataService, EloGetService eloGetService, MessageSender messageSender) {
        this.dataService = dataService;
        this.eloGetService=eloGetService;
        this.messageSender = messageSender;
        //scheduler.scheduleAtFixedRate(this::printChatRoomData, 0, 20, TimeUnit.SECONDS);
    }
    /*
    private void printChatRoomData() {
        // chatRoomIDMap 출력
        System.out.println("ChatRoomIDMap:");
        chatRoomIDMap.forEach((id, room) -> {
            System.out.println("ChatRoom ID: " + id);
            List<String> nickNames = room.getNickNames();
            System.out.println("Nicknames: " + String.join(", ", nickNames));
        });

        // chatRoomGroupNameMap 출력
        System.out.println("ChatRoomGroupNameMap:");
        chatRoomGroupNameMap.forEach((groupName, roomMap) -> {
            System.out.println("Group Name: " + groupName);
            roomMap.forEach((subGroup, rooms) -> {
                System.out.println("  Subgroup: " + subGroup);
                rooms.forEach(room -> {
                    List<String> nickNames = room.getNickNames();
                    System.out.println("Nicknames: " + String.join(", ", nickNames));
                });
            });
        });
    }*/

    public List<ChatRoom> getRoomsByGroupName(String groupName){
        Map<String, List<ChatRoom>> currentGroup = chatRoomGroupNameMap.get(groupName);
        if(currentGroup==null){
            return Collections.emptyList();
        }
        return currentGroup.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    public List<ChatRoomBaseConfig> getRoomBaseConfig(List<ChatRoom> chatRooms){
        List<ChatRoomBaseConfig> roomBaseConfigs = new ArrayList<>();
        for (ChatRoom chatRoom : chatRooms) {
            ChatRoomBaseConfig baseConfig = new ChatRoomBaseConfig();
            baseConfig.setRoomName(chatRoom.getRoomName());
            baseConfig.setCategoryName(chatRoom.getTopic());
            baseConfig.setIsPassword(chatRoom.getPassword() !="");
            baseConfig.setRoomID(chatRoom.getRoomID());
            baseConfig.setMaxUserNum(chatRoom.getMaxUser());
            baseConfig.setCurrentUserNum(chatRoom.getUserNumber());
            baseConfig.setTime(chatRoom.getTimeLimit());
            roomBaseConfigs.add(baseConfig);
        }
        return roomBaseConfigs;
    }
    public List<ChatRoom> getRoomsByGroupNameAndCategory(String groupName,String categoryName){
        List<ChatRoom> chatRooms = new ArrayList<>(chatRoomGroupNameMap.get(groupName).get(categoryName));
        return chatRooms;
    }

    public String createRoom(ChatRoomConfig chatRoomConfig) {
        this.configValidation(chatRoomConfig);
        String currentRoomID;
        ChatRoom currentRoom;
        List<String> imageSet= dataService.getRandomData(chatRoomConfig.getGroupName(), chatRoomConfig.getTopic(), chatRoomConfig.getImageNum());
        if(imageSet.size()!=chatRoomConfig.getImageNum()){
            throw new InvalidRoomConfigException();
        }
        currentRoomID = UUID.randomUUID().toString();
        currentRoom = new ChatRoom(chatRoomConfig,currentRoomID,imageSet,colors);
        chatRoomIDMap.putIfAbsent(currentRoomID,currentRoom);
        AtomicInteger state = currentRoom.getState();
        scheduler.schedule(() -> {
            if (state.compareAndSet(0, -1)) {
                chatRoomIDMap.remove(currentRoomID);
            }
        }, 10, TimeUnit.SECONDS);
        return currentRoomID;
    }

    public void configValidation(ChatRoomConfig chatRoomConfig){
        if (chatRoomConfig.getGroupName()==null){
            throw new InvalidRoomConfigException();
        }
        if(chatRoomConfig.getTopic()==null){
            throw new InvalidRoomConfigException();
        }
        if (chatRoomConfig.getTimeLimit()==null || chatRoomConfig.getTimeLimit()<minTime || chatRoomConfig.getTimeLimit()>maxTime){
            throw new InvalidRoomConfigException();
        }
        if(chatRoomConfig.getImageNum()==null || chatRoomConfig.getImageNum()<minImage|| chatRoomConfig.getImageNum()>maxImage){
            throw new InvalidRoomConfigException();
        }
        if(chatRoomConfig.getUserLimit()==null||chatRoomConfig.getUserLimit()>maxUser || chatRoomConfig.getUserLimit()<1){
            throw new InvalidRoomConfigException();
        }
        if(chatRoomConfig.getImageNum()>dataService.getDataNum(chatRoomConfig.getGroupName(),chatRoomConfig.getTopic())){
            throw new InvalidRoomConfigException();
        }
    }
    public ChatRoom getRoom(String roomID){
        ChatRoom currentRoom = chatRoomIDMap.get(roomID);
        if (currentRoom==null){
            throw new InvalidInputException("채팅방이 존재하지 않습니다.");
        }
        return currentRoom;
    }
    public void checkUserValidation(String roomId,String password,String userID){
        ChatRoom chatRoom = this.getRoom(roomId);
        chatRoom.checkUserLimit();
        chatRoom.checkPassword(password);
        chatRoom.checkBanList(userID);
        chatRoom.checkUser(userID);
        this.checkRoomState(chatRoom);
    }
    public void addUser(String roomID,String userID){
        ChatRoom currentRoom = getRoom(roomID);
        Map<String, List<ChatRoom>> categoryMap = chatRoomGroupNameMap.computeIfAbsent(currentRoom.getGroupName(), k -> new ConcurrentHashMap<>());
        List<ChatRoom> chatRooms=categoryMap.computeIfAbsent(currentRoom.getTopic(),k -> Collections.synchronizedList(new ArrayList<>()));
        currentRoom.addUser(userID);
        AtomicInteger state = currentRoom.getState();
        if (state.compareAndSet(0, 1)) {
            chatRooms.add(currentRoom);
        }
    }
    public void removeUser(String roomID,String userID){
        ChatRoom currentRoom = getRoom(roomID);
        currentRoom.removeUser(userID);
        if (currentRoom.getState().get()==-1){
            Map<String, List<ChatRoom>> categoryMap= chatRoomGroupNameMap.get(currentRoom.getGroupName());
            List<ChatRoom> chatRooms=categoryMap.get(currentRoom.getTopic());
            chatRooms.remove(currentRoom);
            chatRoomIDMap.remove(currentRoom.getRoomID());
        }
    }

    private void checkRoomState(ChatRoom chatRoom){
        if(chatRoom.getState().get()>=2){
            throw new RoomFullException();
        }
    }
    public String getUserColor(String roomID,String userID){
        ChatRoom room = chatRoomIDMap.get(roomID);
        return room.getColor(userID);
    }
    public List<String> getNickNames(String roomID){
        ChatRoom room = chatRoomIDMap.get(roomID);
        return room.getNickNames();
    }

    public void startGame(String roomID,String userID){
        ChatRoom room = chatRoomIDMap.get(roomID);
        if(room.isHost(userID)){
            Game game = new Game(room.getRoomID(),room.getImages(),room.getUserIDColor(),room.getTimeLimit(),room.getGroupName(),room.getTopic(),eloGetService,messageSender);
            room.startGame(game);
            Map<String, List<ChatRoom>> categoryMap= chatRoomGroupNameMap.get(room.getGroupName());
            List<ChatRoom> chatRooms=categoryMap.get(room.getTopic());
            chatRooms.remove(room);


        }
    }

    public void vote(String userID,String roomID, VotePayload votePayload){
        ChatRoom room = chatRoomIDMap.get(roomID);
        room.vote(userID,votePayload.isVoteForFirst(),votePayload.getRound());
    }

    /*
    public String createRoom(ChatRoomConfig chatRoomConfig) {
        String currentRoomID;
        ChatRoom currentRoom;
        List<String> imageSet= dataService.getRandomData(chatRoomConfig.getGroupName(), chatRoomConfig.getTopic(), chatRoomConfig.getImageNum());
        if (imageSet.size()<chatRoomConfig.getImageNum()){
            throw new InvalidRoomConfigException();
        }
        do {
            currentRoomID = UUID.randomUUID().toString();
            currentRoom = new ChatRoom(chatRoomConfig,currentRoomID,imageSet,colors);
        } while (chatRoomIDMap.putIfAbsent(currentRoomID,currentRoom)!=null);
        final ChatRoom finalRoom=currentRoom;
        scheduler.schedule(() -> {
            if (finalRoom.getState().compareAndSet(0, -1)) {
                chatRoomIDMap.remove(finalRoom.getRoomID());
            }
        }, 10, TimeUnit.SECONDS);
        return currentRoomID;
    }

    public void removeUser(String roomID,String sessionID){
        ChatRoom room = chatRoomIDMap.get(roomID);
        if (room==null){
            return;
        }
        String userID = sessionUserManager.getUserID(sessionID);
        room.removeUser(userID);
    }
    public String getUserColor(String roomID,String sessionID){
        ChatRoom room = chatRoomIDMap.get(roomID);
        String userID = sessionUserManager.getUserID(sessionID);
        return room.getColor(userID);
    }
    public List<String> getNickNames(String roomID){
        ChatRoom room = chatRoomIDMap.get(roomID);
        return room.getNickNames();
    }


    public void checkSession(String roomID, String sessionID){
        ChatRoom room = chatRoomIDMap.get(roomID);
        room.checkSession(sessionID);
    }

    public void addUser(String roomID,String sessionID,String password){
        ChatRoom currentRoom = getRoom(roomID);
        checkRoomState(currentRoom);
        String userID = sessionUserManager.getUserID(sessionID);
        currentRoom.checkBanList(userID);
        Map<String, Set<ChatRoom>> categoryMap = chatRoomGroupNameMap.computeIfAbsent(currentRoom.getGroupName(), k -> new ConcurrentHashMap<>());
        Set<ChatRoom> chatRooms=categoryMap.computeIfAbsent(currentRoom.getTopic(),k->ConcurrentHashMap.newKeySet());
        currentRoom.addUser(sessionID,userID,password);
        messageSender.configSend(sessionID,roomID,currentRoom.getNickName(sessionID));
        checkHost(currentRoom,sessionID);
        chatRooms.add(currentRoom);
    }

    private ChatRoom getRoom(String roomID){
        ChatRoom currentRoom = chatRoomIDMap.get(roomID);
        if (currentRoom==null){
            throw new InvalidInputException();
        }
        return currentRoom;
    }
    private void checkRoomState(ChatRoom chatRoom){
        if(chatRoom.getState().get()>=2){
            throw new RoomFullException();
        }
    }
    private void checkHost(ChatRoom chatRoom,String sessionID){
        if(chatRoom.getState().compareAndSet(0,1)){
            chatRoom.setHostSessionID(sessionID);
            messageSender.authSend(sessionID,chatRoom.getRoomID());
        }
    }
    public List<ChatRoom> getRoomsByGroupName(String groupName){
        Map<String, Set<ChatRoom>> currentGroup = chatRoomGroupNameMap.get(groupName);
        return currentGroup.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }
    public List<ChatRoom> getRoomsByGroupNameAndCategory(String groupName,String categoryName){
        List<ChatRoom> chatRooms = new ArrayList<>(chatRoomGroupNameMap.get(groupName).get(categoryName));
        return chatRooms;
    }

    public List<ChatRoomBaseConfig> getRoomBaseConfig(List<ChatRoom> chatRooms){
        List<ChatRoomBaseConfig> roomBaseConfigs = new ArrayList<>();
        for (ChatRoom chatRoom : chatRooms) {
            ChatRoomBaseConfig baseConfig = new ChatRoomBaseConfig();
            baseConfig.setRoomName(chatRoom.getRoomName());
            baseConfig.setCategoryName(chatRoom.getTopic());
            baseConfig.setIsPassword(chatRoom.getPassword() !=null);
            baseConfig.setRoomID(chatRoom.getRoomID());
            baseConfig.setMaxUserNum(chatRoom.getMaxUser());
            baseConfig.setCurrentUserNum(chatRoom.getUserNumber());
            roomBaseConfigs.add(baseConfig);
        }
        return roomBaseConfigs;
    }

    public boolean configValidation(ChatRoomConfig chatRoomConfig){
        if (chatRoomConfig.getGroupName()==null){
            throw new InvalidRoomConfigException();
        }
        if(chatRoomConfig.getTopic()==null){
            throw new InvalidRoomConfigException();
        }
        if (chatRoomConfig.getTimeLimit()==null || chatRoomConfig.getTimeLimit()<minTime || chatRoomConfig.getTimeLimit()>maxTime){
            throw new InvalidRoomConfigException();
        }
        if(chatRoomConfig.getImageNum()==null || chatRoomConfig.getImageNum()<minImage|| chatRoomConfig.getImageNum()>maxImage){
            throw new InvalidRoomConfigException();
        }
        if(chatRoomConfig.getUserLimit()==null||chatRoomConfig.getUserLimit()>maxUser){
            throw new InvalidRoomConfigException();
        }
        return true;
    }
     */
}
