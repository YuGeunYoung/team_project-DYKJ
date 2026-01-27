package com.project.dykj.domain.chat.util;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dykj.domain.chat.dto.ChatMessageVO;
import com.project.dykj.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper; // JSON 변환기 (Spring 기본 내장)
    
    // 현재 접속해 있는 사람들의 세션을 저장할 리스트
    private static List<WebSocketSession> sessionList = new ArrayList<>();

    // 1. 누군가 접속했을 때
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionList.add(session);
        log.info("새로운 채팅 접속자: " + session.getId());
    }

    // 2. 메시지를 보냈을 때 (가장 중요!)
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 2-1. React에서 보낸 JSON 문자열 받기
        String payload = message.getPayload();
        log.info("받은 메시지: " + payload);

        // 2-2. JSON -> Java 객체(VO)로 변환
        ChatMessageVO chatMessage = objectMapper.readValue(payload, ChatMessageVO.class);
        
        // 2-3. DB에 저장
        chatService.saveMessage(chatMessage);

        // 2-4. 접속한 모든 사람에게 다시 뿌려주기 (Broadcasting)
        for (WebSocketSession sess : sessionList) {
            sess.sendMessage(new TextMessage(payload));
        }
    }

    // 3. 접속을 끊었을 때
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionList.remove(session);
        log.info("채팅 접속 해제: " + session.getId());
    }
}