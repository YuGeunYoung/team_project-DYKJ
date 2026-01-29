package com.project.dykj.domain.chat.util;

import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dykj.domain.chat.dto.ChatMessageVO;
import com.project.dykj.domain.chat.service.ChatService;
import com.project.dykj.kis.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final StockService stockService; 
    private final ObjectMapper objectMapper; 
    private static List<WebSocketSession> sessionList = new ArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionList.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            ChatMessageVO chatMessage = objectMapper.readValue(payload, ChatMessageVO.class);
            chatMessage.setSendTime(new Date()); 
            
            // 1. 일반 메시지 저장 및 전송
            chatService.saveMessage(chatMessage);
            broadcastMessage(chatMessage);

            // 2. 봇 기능 (!로 시작할 때만)
            String content = chatMessage.getChatContent();
            if (content != null && content.startsWith("!")) {
                handleStockBot(content.substring(1).trim());
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: ", e);
        }
    }

    private void handleStockBot(String stockId) {
        try {
            Map<?, ?> priceData = stockService.getCurrentPrice(stockId);
            if (priceData != null && !priceData.isEmpty()) {
                String price = String.valueOf(priceData.get("stck_prpr"));
                String rate = String.valueOf(priceData.get("prdy_ctrt"));
                
                ChatMessageVO botMsg = new ChatMessageVO();
                botMsg.setUserId("주식봇🤖");
                botMsg.setChatContent(String.format("🤖 [%s] 현재가: %s원 (등락률: %s%%)", stockId, price, rate));
                botMsg.setSendTime(new Date());
                broadcastMessage(botMsg);
            }
        } catch (Exception e) {
            log.error("봇 응답 생성 실패: ", e);
        }
    }

    private void broadcastMessage(ChatMessageVO vo) throws Exception {
        String json = objectMapper.writeValueAsString(vo);
        TextMessage tm = new TextMessage(json);
        for (WebSocketSession sess : sessionList) {
            if (sess.isOpen()) sess.sendMessage(tm);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionList.remove(session);
    }
}