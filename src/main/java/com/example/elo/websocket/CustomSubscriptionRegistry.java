package com.example.elo.websocket;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.broker.AbstractSubscriptionRegistry;
import org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
//No Use Now.
public class CustomSubscriptionRegistry extends AbstractSubscriptionRegistry {
    public static final int DEFAULT_CACHE_LIMIT = 1024;
    private static final EvaluationContext messageEvalContext = SimpleEvaluationContext.forPropertyAccessors(new PropertyAccessor[]{new CustomSubscriptionRegistry.SimpMessageHeaderPropertyAccessor()}).build();
    private PathMatcher pathMatcher = new AntPathMatcher();
    private int cacheLimit = 1024;
    @Nullable
    private String selectorHeaderName;
    private volatile boolean selectorHeaderInUse;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final CustomSubscriptionRegistry.DestinationCache destinationCache = new CustomSubscriptionRegistry.DestinationCache();
    private final CustomSubscriptionRegistry.SessionRegistry sessionRegistry = new CustomSubscriptionRegistry.SessionRegistry();

    public CustomSubscriptionRegistry() {
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    public void setCacheLimit(int cacheLimit) {
        this.cacheLimit = cacheLimit;
        this.destinationCache.ensureCacheLimit();
    }

    public int getCacheLimit() {
        return this.cacheLimit;
    }

    public void setSelectorHeaderName(@Nullable String selectorHeaderName) {
        this.selectorHeaderName = StringUtils.hasText(selectorHeaderName) ? selectorHeaderName : null;
    }

    @Nullable
    public String getSelectorHeaderName() {
        return this.selectorHeaderName;
    }

    protected void addSubscriptionInternal(String sessionId, String subscriptionId, String destination, Message<?> message) {
        boolean isPattern = this.pathMatcher.isPattern(destination);
        Expression expression = this.getSelectorExpression(message.getHeaders());
        CustomSubscriptionRegistry.Subscription subscription = new CustomSubscriptionRegistry.Subscription(subscriptionId, destination, isPattern, expression);
        this.sessionRegistry.addSubscription(sessionId, subscription);
        this.destinationCache.updateAfterNewSubscription(sessionId, subscription);
    }

    @Nullable
    private Expression getSelectorExpression(MessageHeaders headers) {
        if (this.getSelectorHeaderName() == null) {
            return null;
        } else {
            String selector = NativeMessageHeaderAccessor.getFirstNativeHeader(this.getSelectorHeaderName(), headers);
            if (selector == null) {
                return null;
            } else {
                Expression expression = null;

                try {
                    expression = this.expressionParser.parseExpression(selector);
                    this.selectorHeaderInUse = true;
                    if (this.logger.isTraceEnabled()) {
                        this.logger.trace("Subscription selector: [" + selector + "]");
                    }
                } catch (Throwable var5) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Failed to parse selector: " + selector, var5);
                    }
                }

                return expression;
            }
        }
    }

    protected void removeSubscriptionInternal(String sessionId, String subscriptionId, Message<?> message) {
        CustomSubscriptionRegistry.SessionInfo info = this.sessionRegistry.getSession(sessionId);
        if (info != null) {
            CustomSubscriptionRegistry.Subscription subscription = info.removeSubscription(subscriptionId);
            if (subscription != null) {
                this.destinationCache.updateAfterRemovedSubscription(sessionId, subscription);
            }
        }

    }

    public void unregisterAllSubscriptions(String sessionId) {
        CustomSubscriptionRegistry.SessionInfo info = this.sessionRegistry.removeSubscriptions(sessionId);
        if (info != null) {
            this.destinationCache.updateAfterRemovedSession(sessionId, info);
        }

    }


    protected MultiValueMap<String, String> findSubscriptionsInternal(String destination, Message<?> message) {
        MultiValueMap<String, String> allMatches = this.destinationCache.getSubscriptions(destination);
        if (!this.selectorHeaderInUse) {
            return allMatches;
        } else {
            MultiValueMap<String, String> result = new LinkedMultiValueMap(allMatches.size());
            allMatches.forEach((sessionId, subscriptionIds) -> {
                CustomSubscriptionRegistry.SessionInfo info = this.sessionRegistry.getSession(sessionId);
                if (info != null) {
                    Iterator var6 = subscriptionIds.iterator();

                    while(var6.hasNext()) {
                        String subscriptionId = (String)var6.next();
                        CustomSubscriptionRegistry.Subscription subscription = info.getSubscription(subscriptionId);
                        if (subscription != null && this.evaluateExpression(subscription.getSelector(), message)) {
                            result.add(sessionId, subscription.getId());
                        }
                    }
                }

            });
            return result;
        }
    }

    private boolean evaluateExpression(@Nullable Expression expression, Message<?> message) {
        if (expression == null) {
            return true;
        } else {
            try {
                Boolean result = (Boolean)expression.getValue(messageEvalContext, message, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return true;
                }
            } catch (SpelEvaluationException var4) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Failed to evaluate selector: " + var4.getMessage());
                }
            } catch (Throwable var5) {
                this.logger.debug("Failed to evaluate selector", var5);
            }

            return false;
        }
    }

    private final class DestinationCache {
        private final Map<String, LinkedMultiValueMap<String, String>> destinationCache = new ConcurrentHashMap(1024);
        private final AtomicInteger cacheSize = new AtomicInteger();
        private final Queue<String> cacheEvictionPolicy = new ConcurrentLinkedQueue();

        private DestinationCache() {
        }

        public LinkedMultiValueMap<String, String> getSubscriptions(String destination) {
            LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds = (LinkedMultiValueMap)this.destinationCache.get(destination);
            if (sessionIdToSubscriptionIds == null) {
                sessionIdToSubscriptionIds = (LinkedMultiValueMap)this.destinationCache.computeIfAbsent(destination, (_destination) -> {
                    LinkedMultiValueMap<String, String> matches = this.computeMatchingSubscriptions(destination);
                    this.cacheEvictionPolicy.add(destination);
                    this.cacheSize.incrementAndGet();
                    return matches;
                });
                this.ensureCacheLimit();
            }

            return sessionIdToSubscriptionIds;
        }

        private LinkedMultiValueMap<String, String> computeMatchingSubscriptions(String destination) {
            LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds = new LinkedMultiValueMap();
            CustomSubscriptionRegistry.this.sessionRegistry.forEachSubscription((sessionId, subscription) -> {
                if (subscription.isPattern()) {
                    if (CustomSubscriptionRegistry.this.pathMatcher.match(subscription.getDestination(), destination)) {
                        this.addMatchedSubscriptionId(sessionIdToSubscriptionIds, sessionId, subscription.getId());
                    }
                } else if (destination.equals(subscription.getDestination())) {
                    this.addMatchedSubscriptionId(sessionIdToSubscriptionIds, sessionId, subscription.getId());
                }

            });
            return sessionIdToSubscriptionIds;
        }

        private void addMatchedSubscriptionId(LinkedMultiValueMap<String, String> sessionIdToSubscriptionIds, String sessionId, String subscriptionId) {
            sessionIdToSubscriptionIds.compute(sessionId, (_sessionId, subscriptionIds) -> {
                if (subscriptionIds == null) {
                    return Collections.singletonList(subscriptionId);
                } else {
                    List<String> result = new ArrayList(subscriptionIds.size() + 1);
                    result.addAll(subscriptionIds);
                    result.add(subscriptionId);
                    return result;
                }
            });
        }

        private void ensureCacheLimit() {
            int size = this.cacheSize.get();
            if (size > CustomSubscriptionRegistry.this.cacheLimit) {
                do {
                    if (this.cacheSize.compareAndSet(size, size - 1)) {
                        String head = (String)this.cacheEvictionPolicy.remove();
                        this.destinationCache.remove(head);
                    }
                } while((size = this.cacheSize.get()) > CustomSubscriptionRegistry.this.cacheLimit);
            }

        }

        public void updateAfterNewSubscription(String sessionId, CustomSubscriptionRegistry.Subscription subscription) {
            if (subscription.isPattern()) {
                Iterator var3 = this.destinationCache.keySet().iterator();

                while(var3.hasNext()) {
                    String cachedDestination = (String)var3.next();
                    if (CustomSubscriptionRegistry.this.pathMatcher.match(subscription.getDestination(), cachedDestination)) {
                        this.addToDestination(cachedDestination, sessionId, subscription.getId());
                    }
                }
            } else {
                this.addToDestination(subscription.getDestination(), sessionId, subscription.getId());
            }

        }

        private void addToDestination(String destination, String sessionId, String subscriptionId) {
            this.destinationCache.computeIfPresent(destination, (_destination, sessionIdToSubscriptionIds) -> {
                sessionIdToSubscriptionIds = sessionIdToSubscriptionIds.clone();
                this.addMatchedSubscriptionId(sessionIdToSubscriptionIds, sessionId, subscriptionId);
                return sessionIdToSubscriptionIds;
            });
        }

        public void updateAfterRemovedSubscription(String sessionId, CustomSubscriptionRegistry.Subscription subscription) {
            if (subscription.isPattern()) {
                String subscriptionId = subscription.getId();
                this.destinationCache.forEach((destination, sessionIdToSubscriptionIds) -> {
                    List<String> subscriptionIds = sessionIdToSubscriptionIds.get(sessionId);
                    if (subscriptionIds != null && subscriptionIds.contains(subscriptionId)) {
                        this.removeInternal(destination, sessionId, subscriptionId);
                    }

                });
            } else {
                this.removeInternal(subscription.getDestination(), sessionId, subscription.getId());
            }

        }

        private void removeInternal(String destination, String sessionId, String subscriptionId) {
            this.destinationCache.computeIfPresent(destination, (_destination, sessionIdToSubscriptionIds) -> {
                sessionIdToSubscriptionIds = sessionIdToSubscriptionIds.clone();
                sessionIdToSubscriptionIds.computeIfPresent(sessionId, (_sessionId, subscriptionIdsx) -> {
                    if (subscriptionIdsx.size() == 1 && subscriptionId.equals(subscriptionIdsx.get(0))) {
                        return null;
                    } else {
                        List subscriptionIds = new ArrayList(subscriptionIdsx);
                        subscriptionIds.remove(subscriptionId);
                        return subscriptionIds.isEmpty() ? null : subscriptionIds;
                    }
                });
                return sessionIdToSubscriptionIds;
            });
        }

        public void updateAfterRemovedSession(String sessionId, CustomSubscriptionRegistry.SessionInfo info) {
            Iterator var3 = info.getSubscriptions().iterator();

            while(var3.hasNext()) {
                CustomSubscriptionRegistry.Subscription subscription = (CustomSubscriptionRegistry.Subscription)var3.next();
                this.updateAfterRemovedSubscription(sessionId, subscription);
            }

        }
    }

    private static final class SessionRegistry {
        private final ConcurrentMap<String, CustomSubscriptionRegistry.SessionInfo> sessions = new ConcurrentHashMap();

        private SessionRegistry() {
        }

        @Nullable
        public CustomSubscriptionRegistry.SessionInfo getSession(String sessionId) {
            return (CustomSubscriptionRegistry.SessionInfo)this.sessions.get(sessionId);
        }

        public void forEachSubscription(BiConsumer<String, CustomSubscriptionRegistry.Subscription> consumer) {
            this.sessions.forEach((sessionId, info) -> {
                info.getSubscriptions().forEach((subscription) -> {
                    consumer.accept(sessionId, subscription);
                });
            });
        }

        public void addSubscription(String sessionId, CustomSubscriptionRegistry.Subscription subscription) {
            CustomSubscriptionRegistry.SessionInfo info = (CustomSubscriptionRegistry.SessionInfo)this.sessions.computeIfAbsent(sessionId, (_sessionId) -> {
                return new CustomSubscriptionRegistry.SessionInfo();
            });
            info.addSubscription(subscription);
        }

        @Nullable
        public CustomSubscriptionRegistry.SessionInfo removeSubscriptions(String sessionId) {
            return (CustomSubscriptionRegistry.SessionInfo)this.sessions.remove(sessionId);
        }
    }

    private static final class Subscription {
        private final String id;
        private final String destination;
        private final boolean isPattern;
        @Nullable
        private final Expression selector;

        public Subscription(String id, String destination, boolean isPattern, @Nullable Expression selector) {
            Assert.notNull(id, "Subscription id must not be null");
            Assert.notNull(destination, "Subscription destination must not be null");
            this.id = id;
            this.selector = selector;
            this.destination = destination;
            this.isPattern = isPattern;
        }

        public String getId() {
            return this.id;
        }

        public String getDestination() {
            return this.destination;
        }

        public boolean isPattern() {
            return this.isPattern;
        }

        @Nullable
        public Expression getSelector() {
            return this.selector;
        }

        public boolean equals(@Nullable Object other) {
            boolean var10000;
            if (this != other) {
                label26: {
                    if (other instanceof CustomSubscriptionRegistry.Subscription) {
                        CustomSubscriptionRegistry.Subscription that = (CustomSubscriptionRegistry.Subscription)other;
                        if (this.id.equals(that.id)) {
                            break label26;
                        }
                    }

                    var10000 = false;
                    return var10000;
                }
            }

            var10000 = true;
            return var10000;
        }

        public int hashCode() {
            return this.id.hashCode();
        }

        public String toString() {
            return "subscription(id=" + this.id + ")";
        }
    }

    private static final class SessionInfo {
        private final Map<String, CustomSubscriptionRegistry.Subscription> subscriptionMap = new ConcurrentHashMap();

        private SessionInfo() {
        }

        public Collection<CustomSubscriptionRegistry.Subscription> getSubscriptions() {
            return this.subscriptionMap.values();
        }

        @Nullable
        public CustomSubscriptionRegistry.Subscription getSubscription(String subscriptionId) {
            return (CustomSubscriptionRegistry.Subscription)this.subscriptionMap.get(subscriptionId);
        }

        public void addSubscription(CustomSubscriptionRegistry.Subscription subscription) {
            this.subscriptionMap.putIfAbsent(subscription.getId(), subscription);
        }

        @Nullable
        public CustomSubscriptionRegistry.Subscription removeSubscription(String subscriptionId) {
            return (CustomSubscriptionRegistry.Subscription)this.subscriptionMap.remove(subscriptionId);
        }
    }

    private static class SimpMessageHeaderPropertyAccessor implements PropertyAccessor {
        private SimpMessageHeaderPropertyAccessor() {
        }

        public Class<?>[] getSpecificTargetClasses() {
            return new Class[]{Message.class, MessageHeaders.class};
        }

        public boolean canRead(EvaluationContext context, @Nullable Object target, String name) {
            return true;
        }

        public TypedValue read(EvaluationContext context, @Nullable Object target, String name) {
            Object value;
            if (target instanceof Message message) {
                value = name.equals("headers") ? message.getHeaders() : null;
            } else {
                if (!(target instanceof MessageHeaders)) {
                    throw new IllegalStateException("Expected Message or MessageHeaders.");
                }

                MessageHeaders headers = (MessageHeaders)target;
                SimpMessageHeaderAccessor accessor = (SimpMessageHeaderAccessor)MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
                Assert.state(accessor != null, "No SimpMessageHeaderAccessor");
                if ("destination".equalsIgnoreCase(name)) {
                    value = accessor.getDestination();
                } else {
                    value = accessor.getFirstNativeHeader(name);
                    if (value == null) {
                        value = headers.get(name);
                    }
                }
            }

            return new TypedValue(value);
        }

        public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
            return false;
        }

        public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object value) {
        }
    }
}

