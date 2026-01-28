package com.project.dykj.domain.chat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    // 채팅 내역 가져오기 주소: GET http://localhost:8080/dykj/api/chat/list
    @GetMapping("/list")
    public List<ChatMessageVO> getChatList() {
        return chatService.getChatList();
    }
}