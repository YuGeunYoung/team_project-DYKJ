package com.project.dykj.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.project.dykj.domain.chat.util.ChatHandler; // 곧 만들 클래스

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketConfigurer {

    private final ChatHandler chatHandler; // 채팅 메시지를 처리할 핵심 로직

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 클라이언트(React)가 접속할 주소를 "/ws-chat"으로 설정
        registry.addHandler(chatHandler, "/ws-chat")
                .setAllowedOrigins("*"); // 테스트를 위해 모든 도메인 허용
    }
}