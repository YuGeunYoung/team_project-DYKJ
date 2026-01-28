package com.project.dykj.domain.chat.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        // 1. React에서 보낸 데이터(아이디, 내용) 받기
        String payload = message.getPayload();
        ChatMessageVO chatMessage = objectMapper.readValue(payload, ChatMessageVO.class);
        
        // 2. [핵심] 서버의 현재 시간을 Java 객체에 직접 넣어줍니다.
        // 이렇게 해야 실시간 메시지에도 시간 정보가 포함됩니다.
        chatMessage.setSendTime(new Date()); 
        
        // 3. DB 저장 (기존과 동일)
        chatService.saveMessage(chatMessage);

        // 4. [수정] 시간이 포함된 Java 객체를 다시 JSON 문자열로 바꿉니다.
        String updatedPayload = objectMapper.writeValueAsString(chatMessage);

        // 5. 접속한 모든 사람에게 '시간이 포함된' 메시지를 뿌려줍니다.
        for (WebSocketSession sess : sessionList) {
            sess.sendMessage(new TextMessage(updatedPayload));
        }
    }

    // 3. 접속을 끊었을 때
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionList.remove(session);
        log.info("채팅 접속 해제: " + session.getId());
    }
}