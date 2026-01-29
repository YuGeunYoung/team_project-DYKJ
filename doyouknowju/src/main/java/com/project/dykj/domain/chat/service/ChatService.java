package com.project.dykj.domain.chat.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.dykj.domain.chat.dto.ChatMessageVO;
import com.project.dykj.domain.chat.repository.ChatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    // 메시지 저장 서비스
    public void saveMessage(ChatMessageVO chatMessage) {
        // 필요하다면 여기서 비속어 필터링 같은 로직을 추가할 수 있습니다.
        chatRepository.insertMessage(chatMessage);
    }
    
 // 기존 saveMessage 메서드 아래에 추가
    public List<ChatMessageVO> getChatList() {
        return chatRepository.selectChatList();
    }
    
    //무한스크롤 방지
    public List<ChatMessageVO> getChatListPaged(Long lastChatId, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("lastChatId", lastChatId);
        params.put("limit", limit);
        return chatRepository.selectChatListPaged(params);
    }
    
    
    
}