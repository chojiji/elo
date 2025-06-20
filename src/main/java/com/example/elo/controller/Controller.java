package com.example.elo.controller;

import com.example.elo.DTO.ChatRoomConfig;
import com.example.elo.DTO.EloData;
import com.example.elo.DTO.GroupChatInfo;
import com.example.elo.DTO.GroupInfo;
import com.example.elo.DTO.InnerDTO.CategoryDataNum;
import com.example.elo.DTO.InnerDTO.ChatRoomBaseConfig;
import com.example.elo.Room.ChatRoomManager;
import com.example.elo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path="/api")
public class Controller {
    private GroupService groupService;
    private ImageGetService imageGetService;
    private DataService dataService;
    private EloGetService eloGetService;
    private CategoryService categoryService;
    private ChatRoomManager chatRoomManager;

    @Autowired
    public Controller(GroupService groupService, ImageGetService imageGetService, DataService dataService, EloGetService eloGetService, CategoryService categoryService, ChatRoomManager chatRoomManager){
        this.groupService = groupService;
        this.imageGetService = imageGetService;
        this.dataService = dataService;
        this.eloGetService = eloGetService;
        this.categoryService = categoryService;
        this.chatRoomManager=chatRoomManager;
    }

    @GetMapping("/main")
    public ResponseEntity<List<GroupInfo>> getGroup(){
        //가능한 그룹들과 그 이미지 제공 1
        List<GroupInfo> groupInfoList= groupService.getGroupInfo();
        imageGetService.getMainImageToDTO(groupInfoList,true);
        return ResponseEntity.ok(groupInfoList);
    }

    @GetMapping("/room/group")
    public ResponseEntity<GroupChatInfo> getGroupChatInfo(String groupName){
        //현재 그룹에서 생성된 채팅방들과 소주제들의 기본정보 제공
        List<ChatRoomBaseConfig> chatRoomBaseConfigList = chatRoomManager.getRoomBaseConfig(chatRoomManager.getRoomsByGroupName(groupName));
        List<CategoryDataNum> categoryDataNum = categoryService.getCategoryDataNum(groupName);
        GroupChatInfo groupChatInfo = new GroupChatInfo(chatRoomBaseConfigList,categoryDataNum);
        return ResponseEntity.ok(groupChatInfo);
    }
    @GetMapping("/room/category")
    public ResponseEntity<List<ChatRoomBaseConfig>> getCategoryChatInfo(String groupName,String categoryName){
        //현재 그룹의 소주제에 해당하는 채팅방들 제공
        List<ChatRoomBaseConfig> chatRoomBaseConfigList =  chatRoomManager.getRoomBaseConfig(chatRoomManager.getRoomsByGroupNameAndCategory(groupName,categoryName));
        return ResponseEntity.ok(chatRoomBaseConfigList);
    }


    @PostMapping("/createRoom")
    public ResponseEntity<String> createRoom(@RequestBody ChatRoomConfig chatRoomConfig){
        String roomId = chatRoomManager.createRoom(chatRoomConfig);
        return ResponseEntity.ok(roomId);
    }

    @GetMapping("/data")
    public ResponseEntity<List<String>> getData(String groupName) {
        //해당 그룹의 개체들 제공 2
        List<String> data = dataService.getDataName(groupName);
        return ResponseEntity.ok(data);
    }
    @GetMapping("/generate-cookie")
    public ResponseEntity<String> generateCookie() {
        String uuid = UUID.randomUUID().toString();

        ResponseCookie cookie = ResponseCookie.from("userTempID", uuid)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofHours(1))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("쿠키가 생성되었습니다.");
    }


    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleDataNotFoundException(IOException e) {
        return ResponseEntity.status(500).body(e.getMessage());
    }
}
