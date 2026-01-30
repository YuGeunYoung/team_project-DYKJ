package com.project.dykj.domain.chat.util;

import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dykj.domain.chat.dto.ChatMessageVO;
import com.project.dykj.domain.chat.service.ChatService;
import com.project.dykj.kis.service.StockService;
import com.project.dykj.kis.model.vo.StockSuggestItem; // [추가] 검색 결과 객체
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

            // 1. 일반 채팅 메시지 저장 및 전송
            chatService.saveMessage(chatMessage);
            broadcastMessage(chatMessage);

            // 2. 봇 기능 실행 (!로 시작하는 경우)
            String content = chatMessage.getChatContent();
            if (content != null && content.startsWith("!")) {
                handleStockBot(content.substring(1).trim());
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생: ", e);
        }
    }

    private void handleStockBot(String query) {
        try {
            // [추가] 점심 메뉴 추천 기능
            if ("점심메뉴".equals(query)) {
                String[] menus = { "제육볶음", "돈까스", "김치찌개", "국밥", "짜장면", "초밥", "햄버거", "백반", "칼국수", "비빔밥" };
                int randomIndex = (int) (Math.random() * menus.length);
                String selectedMenu = menus[randomIndex];

                ChatMessageVO botMsg = new ChatMessageVO();
                botMsg.setUserId("주식봇🤖");
                botMsg.setChatContent("오늘 점심 메뉴는 [" + selectedMenu + "] 어떠신가요?");
                botMsg.setSendTime(new Date());

                broadcastMessage(botMsg);
                return; // 여기서 함수 종료 (주식 검색 안 함)
            }
            // [검증 단계] 팀장님 제안: suggest()로 종목 찾기
            List<StockSuggestItem> candidates = stockService.suggest(query, 1);

            // 검색 결과가 없으면 (예: !ㅋㅋㅋ, !!!) 응답하지 않고 종료
            if (candidates == null || candidates.isEmpty()) {
                return;
            }

            // 검색된 첫 번째 종목 정보 추출
            StockSuggestItem target = candidates.get(0);
            String realStockId = target.getStockId();
            String realStockName = target.getStockName();

            // [조회 단계] KIS API에서 실시간 가격 조회
            Map<?, ?> priceData = stockService.getCurrentPrice(realStockId);

            if (priceData != null && priceData.containsKey("output")) {
                // "output" 포장지를 뜯어 내용물 꺼내기
                Map<String, Object> output = (Map<String, Object>) priceData.get("output");

                String price = String.valueOf(output.get("stck_prpr")); // 현재가
                String rate = String.valueOf(output.get("prdy_ctrt")); // 등락률

                // 봇 메시지 구성
                String botReply = String.format("🤖 [%s] %s\n현재가: %s원 (등락률: %s%%)",
                        realStockId, realStockName, price, rate);

                ChatMessageVO botMsg = new ChatMessageVO();
                botMsg.setUserId("주식봇🤖");
                botMsg.setChatContent(botReply);
                botMsg.setSendTime(new Date());

                broadcastMessage(botMsg);
            }
        } catch (Exception e) {
            log.error("봇 응답 생성 중 에러: ", e);
        }
    }

    private void broadcastMessage(ChatMessageVO vo) throws Exception {
        String json = objectMapper.writeValueAsString(vo);
        TextMessage tm = new TextMessage(json);
        for (WebSocketSession sess : sessionList) {
            if (sess.isOpen())
                sess.sendMessage(tm);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionList.remove(session);
    }
}