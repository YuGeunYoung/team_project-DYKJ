package com.project.dykj.domain.chat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.chat.dto.ChatMessageVO;
import com.project.dykj.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat") // API 주소의 시작점
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // 리엑트 5173주소는 우리와 함께 한다는 뜻 보안을위해 쓰는 코드
public class ChatController {

    private final ChatService chatService;

 // 두 개의 getChatList를 하나로 통합!
    @GetMapping("/list")
    public List<ChatMessageVO> getChatList(
            @RequestParam(required = false) Long lastChatId,
            @RequestParam(defaultValue = "100") int limit) {
        
        // lastChatId가 null이면 최신 100개를 가져오고, 
        // lastChatId가 있으면 그 이전(과거) 데이터를 가져옵니다.
        return chatService.getChatListPaged(lastChatId, limit);
    }
    
}