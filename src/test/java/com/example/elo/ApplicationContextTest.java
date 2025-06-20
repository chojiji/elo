package com.example.elo;

import com.example.elo.Room.ChatRoomManager;
import com.example.elo.controller.ChatController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ApplicationContextTest {

    @Autowired
    private ChatRoomManager chatRoomManager;

    @Autowired
    private ChatController chatController;

    @Test
    public void contextLoads() {
        assertThat(chatRoomManager).isNotNull();
        assertThat(chatController).isNotNull();
    }
}