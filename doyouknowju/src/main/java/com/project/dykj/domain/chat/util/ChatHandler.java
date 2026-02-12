package com.project.dykj.domain.chat.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dykj.domain.chat.dto.ChatMessageVO;
import com.project.dykj.domain.chat.service.ChatService;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.service.MemberService;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.service.StockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final MemberService memberService; // 유저 정보 조회를 위해 주입
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

            // [추가] 가입 7일 제한 로직 (한국식: 가입일 = 1일차)
            Member sender = memberService.getMemberById(chatMessage.getUserId());
            if (sender != null && !"ADMIN".equals(sender.getUserRole())) { // 관리자는 제외 가능
                long diffInMillies = Math.abs(new Date().getTime() - sender.getEnrollDate().getTime());
                long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1; // 당일 1일차 적용

                if (diffInDays < 7) {
                    ChatMessageVO systemMsg = new ChatMessageVO();
                    systemMsg.setUserId("시스템🤖");
                    systemMsg.setChatContent("신규 회원은 가입 7일 후부터 채팅이 가능합니다. (현재 " + diffInDays + "일차)");
                    systemMsg.setSendTime(new Date());

                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(systemMsg)));
                    return; // 로직 종료: 저장 및 브로드캐스트 안 함
                }

                // [추가] 칭호 정보 설정
                chatMessage.setUserTitle(sender.getEquippedTitleName());
                chatMessage.setUserTitleColor(sender.getEquippedTitleColor());
                chatMessage.setUserTitleImgUrl(sender.getEquippedTitleImgUrl());
            }

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
            if ("점심메뉴".equals(query)) {
                String[] menus = { "제육볶음", "돈까스", "김치찌개", "국밥", "짜장면", "초밥", "햄버거", "백반", "칼국수", "비빔밥" };
                int randomIndex = (int) (Math.random() * menus.length);
                String selectedMenu = menus[randomIndex];

                ChatMessageVO botMsg = new ChatMessageVO();
                botMsg.setUserId("주식봇🤖");
                botMsg.setChatContent("오늘 점심 메뉴는 [" + selectedMenu + "] 어떠신가요?");
                botMsg.setSendTime(new Date());

                broadcastMessage(botMsg);
                return;
            }
            List<StockSuggestItem> candidates = stockService.suggest(query, 1);
            if (candidates == null || candidates.isEmpty())
                return;

            StockSuggestItem target = candidates.get(0);
            Map<?, ?> priceData = stockService.getCurrentPrice(target.getStockId());

            if (priceData != null && priceData.containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) priceData.get("output");
                String botReply = String.format("🤖 [%s] %s\n현재가: %s원 (등락률: %s%%)",
                        target.getStockId(), target.getStockName(), output.get("stck_prpr"), output.get("prdy_ctrt"));

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