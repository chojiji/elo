package com.example.elo.service;

import com.example.elo.Room.ChatRoom;
import com.example.elo.Room.ChatRoomManager;
import com.example.elo.message.ConfigMessage;
import com.example.elo.message.EventMessage;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.List;
import java.util.Set;


@Aspect
@Component
public class EventListenerAspect {
    private SimpUserRegistry simpUserRegistry;
    private ChatRoomManager chatRoomManager;
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public EventListenerAspect(SimpUserRegistry simpUserRegistry,ChatRoomManager chatRoomManager, SimpMessagingTemplate simpMessagingTemplate){
        this.simpUserRegistry=simpUserRegistry;
        this.chatRoomManager=chatRoomManager;
        this.simpMessagingTemplate=simpMessagingTemplate;
    }
    /*

    // Pointcut: org.springframework.web.socket.messaging.DefaultSimpUserRegistry 클래스의 onApplicationEvent 메서드
    @Pointcut("execution(public void org.springframework.web.socket.messaging.DefaultSimpUserRegistry.onApplicationEvent(org.springframework.context.ApplicationEvent)) && args(event)")
    public void onApplicationEventPointcut(ApplicationEvent event) {}

    // Advice: onApplicationEvent 메서드가 호출되기 전에 실행되는 코드
    @Before("onApplicationEventPointcut(event)")
    public void beforeOnApplicationEvent(ApplicationEvent event) {
        // event 객체를 활용할 수 있음
        System.out.println("total work!! and sleep");
        System.out.println("Event details: " + event.toString());
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            System.out.println(element);
        }
        try {
            // 1초 동안 일시 정지 (1000ms)
            Thread.sleep(1000);  // 1000 밀리초 = 1초
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("total work!! and sleep fin");
        if (event instanceof SessionDisconnectEvent){
            AbstractSubProtocolEvent subProtocolEvent = (AbstractSubProtocolEvent)event;
            String userName = subProtocolEvent.getUser().getName();
            String stompID = SimpMessageHeaderAccessor.getSessionId(subProtocolEvent.getMessage().getHeaders());
            System.out.println("Before handling event: " + event);
            System.out.println(subProtocolEvent.getUser().getName());
            Set<SimpSubscription> simpSubscriptionSet = simpUserRegistry.getUser(userName).getSession(stompID).getSubscriptions();
            for(SimpSubscription simpSubscription:simpSubscriptionSet){
                String destination = simpSubscription.getDestination();
                if (destination.startsWith("/chat")) { // /chat으로 시작하는 것만 처리
                    destination = destination.substring(destination.length() - 36);
                    chatRoomManager.removeUser(destination, userName);
                }
            }
        }
    }*/

    @EventListener
    @Order(1)
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String userName = event.getUser().getName();
        AbstractSubProtocolEvent subProtocolEvent = (AbstractSubProtocolEvent)event;
        String stompID = SimpMessageHeaderAccessor.getSessionId(subProtocolEvent.getMessage().getHeaders());
        Set<SimpSubscription> simpSubscriptionSet = simpUserRegistry.getUser(userName).getSession(stompID).getSubscriptions();
        for(SimpSubscription simpSubscription:simpSubscriptionSet){
            String destination = simpSubscription.getDestination();
            if (destination.startsWith("/chat")) {
                String roomID = destination.substring(destination.length() - 36);
                String nickName = chatRoomManager.getUserColor(roomID,userName);
                chatRoomManager.removeUser(roomID, userName);
                List<String> nickNames = chatRoomManager.getNickNames(roomID);
                EventMessage exitMessage = new EventMessage(false, nickName, nickNames);
                simpMessagingTemplate.convertAndSend(destination, exitMessage);
            }
        }
    }

    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void handleSubscription(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        Principal principal = headerAccessor.getUser();

        if (destination != null && principal != null) {
            String userID = principal.getName();

            if (destination.startsWith("/chat/")) {
                String roomID = destination.substring("/chat/".length());
                String nickName = chatRoomManager.getUserColor(roomID, userID);
                List<String> nickNames = chatRoomManager.getNickNames(roomID);
                EventMessage initMessage = new EventMessage(true, nickName, nickNames);
                simpMessagingTemplate.convertAndSend(destination, initMessage);
            } else if (destination.startsWith("/user/queue/")) {
                // queue 방식 (1:1 메시징)
                String queueName = destination.substring("/user/queue/".length());
                String[] parts = queueName.split("/");
                String roomID = parts[0];
                String nickName = chatRoomManager.getUserColor(roomID,userID);
                ConfigMessage configMessage = new ConfigMessage(nickName);
                simpMessagingTemplate.convertAndSendToUser(userID, "/queue/" + queueName, configMessage);
            }
        }
    }
}
