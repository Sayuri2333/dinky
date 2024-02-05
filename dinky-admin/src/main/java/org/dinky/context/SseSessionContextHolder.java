/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.context;

import org.dinky.data.constant.SseConstant;
import org.dinky.data.exception.BusException;
import org.dinky.data.vo.SseDataVo;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SseSessionContextHolder {
    // 使用一个Map来存储<Session id, 订阅者>信息
    private static final Map<String, TopicSubscriber> sessionMap = new ConcurrentHashMap<>();

    /**
     * Subscribes a session to the topics.
     * A session can subscribe to multiple topics
     *
     * @param sessionId The ID of the session.
     * @param topics    The list of topics to subscribe to.
     * @return The updated set of topics for the session.
     * @throws BusException If the session does not exist.
     */
    public static Set<String> subscribeTopic(String sessionId, List<String> topics) {
        if (exists(sessionId)) {
            return sessionMap.get(sessionId).updateTopics(topics);
        } else {
            HashSet<String> reconnectMessage = new HashSet<>(1);
            reconnectMessage.add(SseConstant.SSE_SESSION_INVALID);
            log.warn("session id is invalid");
            return reconnectMessage;
        }
    }

    /**
     * Connects a session with the given session key.
     *
     * @param sessionKey The session key of the new session.
     * @return The SseEmitter for the session.
     */
    public static SseEmitter connectSession(String sessionKey) {
        log.debug("New session wants to connect: {}", sessionKey);
        if (exists(sessionKey)) {
            log.warn("Session key already exists: {}", sessionKey);
            closeSse(sessionKey);
        }
        // 创建一个sse发送者，补充这个发送者在发送失败、超时、完成时的方法。设置超时时间为10分钟
        SseEmitter sseEmitter = new SseEmitter(60 * 1000L * 10);
        sseEmitter.onError(err -> onError(sessionKey, err));
        sseEmitter.onTimeout(() -> onTimeout(sessionKey));
        sseEmitter.onCompletion(() -> onCompletion(sessionKey));
        try {
            // Set the client reconnection interval, 0 to reconnect immediately
            // 使用SseEmitter.event()返回一个事件builder，设置其超时时间为1秒。实际发送的是空事件，用来测试
            sseEmitter.send(SseEmitter.event().reconnectTime(1000));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 初始化一个订阅者到map中
        sessionMap.put(sessionKey, TopicSubscriber.of(sseEmitter));
        return sseEmitter;
    }

    /**
     * Checks if a session with the given session key exists.
     *
     * @param sessionKey The session key to check.
     * @return true if the session exists, false otherwise.
     */
    public static boolean exists(String sessionKey) {
        return sessionMap.get(sessionKey) != null;
    }

    /**
     * Handles the timeout event for a session.
     *
     * @param sessionKey The session key of the timed-out session.
     */
    public static void onTimeout(String sessionKey) {
        log.debug("Type: SseSession Timeout, Session ID: {}", sessionKey);
        closeSse(sessionKey);
    }

    /**
     * Closes the SseEmitter for a session.
     *
     * @param sessionKey The session key of the session to close.
     */
    // 如果存在map中就关闭，然后remove
    public static void closeSse(String sessionKey) {
        if (exists(sessionKey)) {
            try {
                // 关闭
                sessionMap.get(sessionKey).getEmitter().complete();
            } catch (Exception e) {
                log.warn("Failed to complete sseEmitter, Session Key: {}, Error: {}", sessionKey, e.getMessage());
            } finally {
                sessionMap.remove(sessionKey);
            }
        }
    }

    /**
     * Handles the error event for a session.
     *
     * @param sessionKey The session key of the session with the error.
     * @param throwable  The throwable representing the error.
     */
    public static void onError(String sessionKey, Throwable throwable) {
        log.error("Type: SseSession [{}] Error, Message: {}", sessionKey, throwable.getMessage());
        if (exists(sessionKey)) {
            try {
                // completeWithError会关闭并返回异常对象
                sessionMap.get(sessionKey).getEmitter().completeWithError(throwable);
            } catch (Exception e) {
                log.error("Failed to complete Sse With Error: {}", e.getMessage());
            }
        }
        // 移除
        sessionMap.remove(sessionKey);
    }

    /**
     * Handles the completion event for a session.
     *
     * @param sessionKey The session key of the completed session.
     */
    public static void onCompletion(String sessionKey) {
        log.debug("Type: SseSession Completion, Session ID: {}", sessionKey);
        closeSse(sessionKey);
    }

    /**
     * Sends the specified content to all subscribers of the given topic.
     *
     * @param topic   The topic to send the content to.
     * @param content The content to send.
     */
    public static void sendTopic(String topic, Object content) {
        // 向指定的topic发送消息
        sessionMap.forEach((sessionKey, topicSubscriber) -> {
            // 遍历每个session的订阅者，如果发现有订阅这个topic，就生产一个VO然后发送过去
            if (topicSubscriber.getTopics().contains(topic)) {
                try {
                    SseDataVo data = new SseDataVo(sessionKey, topic, content);
                    sendSse(sessionKey, data);
                } catch (Exception e) {
                    log.error("Error sending sse data:{}", e.getMessage());
                    onError(sessionKey, e);
                }
            }
        });
    }

    /**
     * Sends the specified SSE data to the session with the given session key.
     *
     * @param sessionKey The session key of the recipient session.
     * @param content    The SSE data to send.
     * @throws IOException If an I/O error occurs while sending the data.
     */
    public static void sendSse(String sessionKey, SseDataVo content) throws Exception {
        // 发送VO到指定session
        if (exists(sessionKey)) {
            sessionMap.get(sessionKey).getEmitter().send(content);
        } else {
            log.warn("SseEmitter not found for session key: {}", sessionKey);
        }
    }

    /**
     * Represents a topic subscriber with the subscribed topics and the associated SseEmitter.
     * 代表一个订阅者，其中包含了某个session注册的topic以及对应的sse emitter信息
     */
    @Data
    public static class TopicSubscriber {
        private Set<String> topics;
        private final SseEmitter emitter;

        /**
         * Creates a new TopicSubscriber with the specified SseEmitter.
         *
         * @param topics  The initial set of topics.
         * @param emitter The SseEmitter associated with the subscriber.
         */
        public TopicSubscriber(Set<String> topics, SseEmitter emitter) {
            this.topics = topics;
            this.emitter = emitter;
        }

        /**
         * Creates a new TopicSubscriber with the specified SseEmitter and an empty set of topics.
         *
         * @param emitter The SseEmitter associated with the subscriber.
         * @return The created TopicSubscriber.
         */
        public static TopicSubscriber of(SseEmitter emitter) {
            return new TopicSubscriber(new HashSet<>(), emitter);
        }

        /**
         * Updates the topics for the subscriber.
         *
         * @param topics The new list of topics.
         * @return The updated set of topics.
         */
        public Set<String> updateTopics(List<String> topics) {
            // 更新topic列表
            this.topics = new HashSet<>(topics);
            return this.topics;
        }
    }
}
