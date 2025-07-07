package com.example.elo.controller;

import com.example.elo.DTO.ChatRoomConfig;
import com.example.elo.DTO.GroupInfo;
import com.example.elo.DTO.InnerDTO.CategoryDataNum;
import com.example.elo.DTO.InnerDTO.ChatRoomBaseConfig;
import com.example.elo.Room.ChatRoomManager;
import com.example.elo.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Controller.class)
class ControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private GroupService groupService;
    @MockBean
    private ImageGetService imageGetService;
    @MockBean
    private DataService dataService;
    @MockBean
    private EloGetService eloGetService;
    @MockBean
    private CategoryService categoryService;
    @MockBean
    private ChatRoomManager chatRoomManager;

    @Test
    @DisplayName("GET /api/main - 메인 그룹 목록 조회 성공")
    void getGroupTest() throws Exception {
        List<GroupInfo> mockedGroupList = Arrays.asList(
                new GroupInfo("음식", null),
                new GroupInfo("차", null)
        );
        given(groupService.getGroupInfo()).willReturn(mockedGroupList);
        doNothing().when(imageGetService).getMainImageToDTO(any(), anyBoolean());

        mockMvc.perform(get("/api/main"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].groupName").value("음식"))
                .andExpect(jsonPath("$[1].groupName").value("차"));
    }

    @Test
    @DisplayName("GET /api/room/group - 그룹 채팅방 정보 조회 성공")
    void getGroupChatInfoTest() throws Exception {
        String groupName = "testGroup";
        List<ChatRoomBaseConfig> mockedRoomConfigs = Arrays.asList(
                new ChatRoomBaseConfig("roomID1", "첫 번째", "음식", true, 5, 10, 60),
                new ChatRoomBaseConfig("roomID2", "두 번째", "개", false, 3, 8, 30)
        );
        given(chatRoomManager.getRoomBaseConfig(any())).willReturn(mockedRoomConfigs);
        List<CategoryDataNum> mockedCategoryData = Arrays.asList(
                new CategoryDataNum("음식", 50),
                new CategoryDataNum("개", 120)
        );
        given(categoryService.getCategoryDataNum(groupName)).willReturn(mockedCategoryData);
        mockMvc.perform(get("/api/room/group")
                        .param("groupName", groupName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.chatRoomBaseConfigList").isArray())
                .andExpect(jsonPath("$.chatRoomBaseConfigList[0].roomName").value("첫 번째"))
                .andExpect(jsonPath("$.chatRoomBaseConfigList[1].roomName").value("두 번째"))
                .andExpect(jsonPath("$.categoryDataNumList").isArray())
                .andExpect(jsonPath("$.categoryDataNumList[0].categoryName").value("음식"))
                .andExpect(jsonPath("$.categoryDataNumList[1].categoryName").value("개"));
    }
    @Test
    @DisplayName("GET /api/room/category - 특정 카테고리 채팅방 목록 조회 성공")
    void getCategoryChatInfoTest() throws Exception {
        String groupName = "음식";
        String categoryName = "중식";
        List<ChatRoomBaseConfig> mockedRoomConfigs = List.of(
                new ChatRoomBaseConfig("roomID1", "채팅방1", "게임", true, 5, 10, 60)
        );
        given(chatRoomManager.getRoomBaseConfig(any())).willReturn(mockedRoomConfigs);

        // when & then
        mockMvc.perform(get("/api/room/category")
                        .param("groupName", groupName)
                        .param("categoryName", categoryName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].roomName").value("채팅방1"));
    }

    @Test
    @DisplayName("POST /api/createRoom - 유효한 설정으로 채팅방 생성 요청 시 성공한다")
    void createRoomTest() throws Exception {
        ChatRoomConfig newRoomConfig = new ChatRoomConfig();
        newRoomConfig.setGroupName("음식");
        newRoomConfig.setRoomName("음식 스터디방");
        newRoomConfig.setTopic("중식");
        newRoomConfig.setPassword("password123123");
        newRoomConfig.setTimeLimit(60);
        newRoomConfig.setUserLimit(10);
        newRoomConfig.setImageNum(2);
        String expectedRoomId = "new-uuid";
        given(chatRoomManager.createRoom(any(ChatRoomConfig.class))).willReturn(expectedRoomId);
        mockMvc.perform(post("/api/createRoom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoomConfig)))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedRoomId));
    }

    @Test
    @DisplayName("GET /api/data - 그룹 데이터 이름 목록 조회 성공")
    void getDataTest() throws Exception {
        String groupName = "test-group";
        List<String> mockedDataNames = Arrays.asList("음식1", "음식2", "음식3");
        given(dataService.getDataName(groupName)).willReturn(mockedDataNames);
        mockMvc.perform(get("/api/data")
                        .param("groupName", groupName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("음식1"))
                .andExpect(jsonPath("$[1]").value("음식2"));
    }

    @Test
    @DisplayName("임시 사용자 ID 쿠키 생성 성공")
    void generateCookieTest() throws Exception {
        mockMvc.perform(get("/api/generate-cookie"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("userTempID=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(content().string("쿠키가 생성되었습니다."));
    }
}