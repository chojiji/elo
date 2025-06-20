package com.example.elo.websocket;


import java.security.Principal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;

import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SubscriptionRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;

//No Use Now.
public class CustomSimpleBrokerHandler extends AbstractBrokerMessageHandler {
    private static final byte[] EMPTY_PAYLOAD = new byte[0];
    @Nullable
    private PathMatcher pathMatcher;
    @Nullable
    private Integer cacheLimit;
    @Nullable
    private String selectorHeaderName;
    @Nullable
    private TaskScheduler taskScheduler;
    @Nullable
    private long[] heartbeatValue;
    @Nullable
    private MessageHeaderInitializer headerInitializer;
    private SubscriptionRegistry subscriptionRegistry = new CustomSubscriptionRegistry();
    private CustomSubscriptionRegistry customSubscriptionRegistry;
    private final Map<String,CustomSimpleBrokerHandler.SessionInfo> sessions = new ConcurrentHashMap();
    @Nullable
    private ScheduledFuture<?> heartbeatFuture;

    public CustomSimpleBrokerHandler(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel, SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {
        super(clientInboundChannel, clientOutboundChannel, brokerChannel, destinationPrefixes);
        customSubscriptionRegistry=(CustomSubscriptionRegistry) subscriptionRegistry;
    }

    public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
        Assert.notNull(subscriptionRegistry, "SubscriptionRegistry must not be null");
        this.subscriptionRegistry = subscriptionRegistry;
        this.initPathMatcherToUse();
        this.initCacheLimitToUse();
        this.initSelectorHeaderNameToUse();

    }

    public SubscriptionRegistry getSubscriptionRegistry() {
        return this.subscriptionRegistry;
    }

    public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
        this.initPathMatcherToUse();
    }

    private void initPathMatcherToUse() {
        if (this.pathMatcher != null) {
            SubscriptionRegistry var2 = this.subscriptionRegistry;
            if (var2 instanceof CustomSubscriptionRegistry) {
                CustomSubscriptionRegistry defaultRegistry = (CustomSubscriptionRegistry)var2;
                defaultRegistry.setPathMatcher(this.pathMatcher);
            }
        }

    }

    public void setCacheLimit(@Nullable Integer cacheLimit) {
        this.cacheLimit = cacheLimit;
        this.initCacheLimitToUse();
    }

    private void initCacheLimitToUse() {
        if (this.cacheLimit != null) {
            SubscriptionRegistry var2 = this.subscriptionRegistry;
            if (var2 instanceof CustomSubscriptionRegistry) {
                CustomSubscriptionRegistry defaultRegistry = (CustomSubscriptionRegistry)var2;
                defaultRegistry.setCacheLimit(this.cacheLimit);
            }
        }

    }

    public void setSelectorHeaderName(@Nullable String selectorHeaderName) {
        this.selectorHeaderName = selectorHeaderName;
        this.initSelectorHeaderNameToUse();
    }

    private void initSelectorHeaderNameToUse() {
        SubscriptionRegistry var2 = this.subscriptionRegistry;
        if (var2 instanceof CustomSubscriptionRegistry defaultRegistry) {
            defaultRegistry.setSelectorHeaderName(this.selectorHeaderName);
        }

    }

    public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        if (taskScheduler != null && this.heartbeatValue == null) {
            this.heartbeatValue = new long[]{10000L, 10000L};
        }

    }

    @Nullable
    public TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    public void setHeartbeatValue(@Nullable long[] heartbeat) {
        if (heartbeat == null || heartbeat.length == 2 && heartbeat[0] >= 0L && heartbeat[1] >= 0L) {
            this.heartbeatValue = heartbeat;
        } else {
            throw new IllegalArgumentException("Invalid heart-beat: " + Arrays.toString(heartbeat));
        }
    }

    @Nullable
    public long[] getHeartbeatValue() {
        return this.heartbeatValue;
    }

    public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
        this.headerInitializer = headerInitializer;
    }

    @Nullable
    public MessageHeaderInitializer getHeaderInitializer() {
        return this.headerInitializer;
    }

    public void startInternal() {
        this.publishBrokerAvailableEvent();
        if (this.taskScheduler != null) {
            Duration interval = this.initHeartbeatTaskDelay();
            if (interval.toMillis() > 0L) {
                this.heartbeatFuture = this.taskScheduler.scheduleWithFixedDelay(new CustomSimpleBrokerHandler.HeartbeatTask(), interval);
            }
        } else {
            Assert.isTrue(this.getHeartbeatValue() == null || this.getHeartbeatValue()[0] == 0L && this.getHeartbeatValue()[1] == 0L, "Heartbeat values configured but no TaskScheduler provided");
        }

    }

    private Duration initHeartbeatTaskDelay() {
        if (this.getHeartbeatValue() == null) {
            return Duration.ZERO;
        } else {
            return this.getHeartbeatValue()[0] > 0L && this.getHeartbeatValue()[1] > 0L ? Duration.ofMillis(Math.min(this.getHeartbeatValue()[0], this.getHeartbeatValue()[1])) : Duration.ofMillis(this.getHeartbeatValue()[0] > 0L ? this.getHeartbeatValue()[0] : this.getHeartbeatValue()[1]);
        }
    }

    public void stopInternal() {
        this.publishBrokerUnavailableEvent();
        if (this.heartbeatFuture != null) {
            this.heartbeatFuture.cancel(true);
        }

    }

    protected void handleMessageInternal(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        String destination = SimpMessageHeaderAccessor.getDestination(headers);
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        this.updateSessionReadTime(sessionId);
        if (this.checkDestinationPrefix(destination)) {
            SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
            if (SimpMessageType.MESSAGE.equals(messageType)) {
                this.logMessage(message);
                this.sendMessageToSubscribers(destination, message);
            } else if (SimpMessageType.CONNECT.equals(messageType)) {
                this.logMessage(message);
                if (sessionId != null) {
                    if (this.sessions.get(sessionId) != null) {
                        if (this.logger.isWarnEnabled()) {
                            this.logger.warn("Ignoring CONNECT in session " + sessionId + ". Already connected.");
                        }

                        return;
                    }

                    long[] heartbeatIn = SimpMessageHeaderAccessor.getHeartbeat(headers);
                    long[] heartbeatOut = this.getHeartbeatValue();
                    Principal user = SimpMessageHeaderAccessor.getUser(headers);
                    MessageChannel outChannel = this.getClientOutboundChannelForSession(sessionId);
                    this.sessions.put(sessionId, new CustomSimpleBrokerHandler.SessionInfo(sessionId, user, outChannel, heartbeatIn, heartbeatOut));
                    SimpMessageHeaderAccessor connectAck = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
                    this.initHeaders(connectAck);
                    connectAck.setSessionId(sessionId);
                    if (user != null) {
                        connectAck.setUser(user);
                    }

                    connectAck.setHeader("simpConnectMessage", message);
                    connectAck.setHeader("simpHeartbeat", heartbeatOut);
                    Message<byte[]> messageOut = MessageBuilder.createMessage(EMPTY_PAYLOAD, connectAck.getMessageHeaders());
                    this.getClientOutboundChannel().send(messageOut);
                }
            } else if (SimpMessageType.DISCONNECT.equals(messageType)) {
                this.logMessage(message);
                if (sessionId != null) {
                    Principal user = SimpMessageHeaderAccessor.getUser(headers);
                    this.handleDisconnect(sessionId, user, message);
                }
            } else if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
                this.logMessage(message);
                this.subscriptionRegistry.registerSubscription(message);
            } else if (SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
                this.logMessage(message);
                this.subscriptionRegistry.unregisterSubscription(message);
            }

        }
    }

    private void updateSessionReadTime(@Nullable String sessionId) {
        if (sessionId != null) {
            CustomSimpleBrokerHandler.SessionInfo info = (CustomSimpleBrokerHandler.SessionInfo)this.sessions.get(sessionId);
            if (info != null) {
                info.setLastReadTime(System.currentTimeMillis());
            }
        }

    }

    private void logMessage(Message<?> message) {
        if (this.logger.isDebugEnabled()) {
            SimpMessageHeaderAccessor accessor = (SimpMessageHeaderAccessor)MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
            accessor = accessor != null ? accessor : SimpMessageHeaderAccessor.wrap(message);
            Log var10000 = this.logger;
            String var10001 = accessor.getShortLogMessage(message.getPayload());
            var10000.debug("Processing " + var10001);
        }

    }

    private void initHeaders(SimpMessageHeaderAccessor accessor) {
        if (this.getHeaderInitializer() != null) {
            this.getHeaderInitializer().initHeaders(accessor);
        }

    }

    private void handleDisconnect(String sessionId, @Nullable Principal user, @Nullable Message<?> origMessage){
        this.sessions.remove(sessionId);
        this.subscriptionRegistry.unregisterAllSubscriptions(sessionId);
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT_ACK);
        accessor.setSessionId(sessionId);
        if (user != null) {
            accessor.setUser(user);
        }

        if (origMessage != null) {
            accessor.setHeader("simpDisconnectMessage", origMessage);
        }

        this.initHeaders(accessor);
        Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
        this.getClientOutboundChannel().send(message);
    }

    protected void sendMessageToSubscribers(@Nullable String destination, Message<?> message) {
        MultiValueMap<String, String> subscriptions = this.subscriptionRegistry.findSubscriptions(message);
        if (!subscriptions.isEmpty() && this.logger.isDebugEnabled()) {
            this.logger.debug("Broadcasting to " + subscriptions.size() + " sessions.");
        }

        long now = System.currentTimeMillis();
        subscriptions.forEach((sessionId, subscriptionIds) -> {
            Iterator var6 = subscriptionIds.iterator();

            while(true) {
                Message reply;
                CustomSimpleBrokerHandler.SessionInfo info;
                do {
                    if (!var6.hasNext()) {
                        return;
                    }

                    String subscriptionId = (String)var6.next();
                    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                    this.initHeaders(headerAccessor);
                    headerAccessor.setSessionId(sessionId);
                    headerAccessor.setSubscriptionId(subscriptionId);
                    headerAccessor.copyHeadersIfAbsent(message.getHeaders());
                    headerAccessor.setLeaveMutable(true);
                    Object payload = message.getPayload();
                    reply = MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
                    info = (CustomSimpleBrokerHandler.SessionInfo)this.sessions.get(sessionId);
                } while(info == null);

                try {
                    info.getClientOutboundChannel().send(reply);
                } catch (Throwable var16) {
                    if (this.logger.isErrorEnabled()) {
                        this.logger.error("Failed to send " + message, var16);
                    }
                } finally {
                    info.setLastWriteTime(now);
                }
            }
        });
    }

    public String toString() {
        return "CustomSimpleBrokerHandler [" + this.subscriptionRegistry + "]";
    }

    private class HeartbeatTask implements Runnable {
        private HeartbeatTask() {
        }

        public void run() {
            long now = System.currentTimeMillis();
            Iterator var3 = CustomSimpleBrokerHandler.this.sessions.values().iterator();

            while(var3.hasNext()) {
                CustomSimpleBrokerHandler.SessionInfo info = (CustomSimpleBrokerHandler.SessionInfo)var3.next();
                if (info.getReadInterval() > 0L && now - info.getLastReadTime() > info.getReadInterval()) {
                    CustomSimpleBrokerHandler.this.handleDisconnect(info.getSessionId(), info.getUser(), (Message)null);
                }

                if (info.getWriteInterval() > 0L && now - info.getLastWriteTime() > info.getWriteInterval()) {
                    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.HEARTBEAT);
                    accessor.setSessionId(info.getSessionId());
                    Principal user = info.getUser();
                    if (user != null) {
                        accessor.setUser(user);
                    }

                    CustomSimpleBrokerHandler.this.initHeaders(accessor);
                    accessor.setLeaveMutable(true);
                    MessageHeaders headers = accessor.getMessageHeaders();
                    info.getClientOutboundChannel().send(MessageBuilder.createMessage(CustomSimpleBrokerHandler.EMPTY_PAYLOAD, headers));
                }
            }

        }
    }

    private static class SessionInfo {
        private static final long HEARTBEAT_MULTIPLIER = 3L;
        private final String sessionId;
        @Nullable
        private final Principal user;
        private final MessageChannel clientOutboundChannel;
        private final long readInterval;
        private final long writeInterval;
        private volatile long lastReadTime;
        private volatile long lastWriteTime;

        public SessionInfo(String sessionId, @Nullable Principal user, MessageChannel outboundChannel, @Nullable long[] clientHeartbeat, @Nullable long[] serverHeartbeat) {
            this.sessionId = sessionId;
            this.user = user;
            this.clientOutboundChannel = outboundChannel;
            if (clientHeartbeat != null && serverHeartbeat != null) {
                this.readInterval = clientHeartbeat[0] > 0L && serverHeartbeat[1] > 0L ? Math.max(clientHeartbeat[0], serverHeartbeat[1]) * 3L : 0L;
                this.writeInterval = clientHeartbeat[1] > 0L && serverHeartbeat[0] > 0L ? Math.max(clientHeartbeat[1], serverHeartbeat[0]) : 0L;
            } else {
                this.readInterval = 0L;
                this.writeInterval = 0L;
            }

            this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();
        }

        public String getSessionId() {
            return this.sessionId;
        }

        @Nullable
        public Principal getUser() {
            return this.user;
        }

        public MessageChannel getClientOutboundChannel() {
            return this.clientOutboundChannel;
        }

        public long getReadInterval() {
            return this.readInterval;
        }

        public long getWriteInterval() {
            return this.writeInterval;
        }

        public long getLastReadTime() {
            return this.lastReadTime;
        }

        public void setLastReadTime(long lastReadTime) {
            this.lastReadTime = lastReadTime;
        }

        public long getLastWriteTime() {
            return this.lastWriteTime;
        }

        public void setLastWriteTime(long lastWriteTime) {
            this.lastWriteTime = lastWriteTime;
        }
    }
}