package com.project.dykj.domain.chat.repository;

import java.util.List;
import java.util.Map; // 추가
import org.apache.ibatis.annotations.Mapper;

import com.project.dykj.domain.chat.dto.ChatMessageVO;

	@Mapper
	public interface ChatRepository {
	    // 메시지 저장 (성공 시 1 리턴)
	    int insertMessage(ChatMessageVO chatMessage);
	    
	 // List를 쓰기 위해 상단에 import java.util.List; 가 필요합니다.
	    List<ChatMessageVO> selectChatList(); // 전체 채팅 내역 조회
	 // 페이징 조회 추가: lastChatId(기준 ID), limit(가져올 개수)를 담은 Map 전달
	    List<ChatMessageVO> selectChatListPaged(Map<String, Object> params);
	}
	
	
	
	
	
	

