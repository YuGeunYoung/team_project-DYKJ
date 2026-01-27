package com.project.dykj.domain.chat.repository;

import org.apache.ibatis.annotations.Mapper;

import com.project.dykj.domain.chat.dto.ChatMessageVO;

	@Mapper
	public interface ChatRepository {
	    // 메시지 저장 (성공 시 1 리턴)
	    int insertMessage(ChatMessageVO chatMessage);
	}
	
	
	
	

