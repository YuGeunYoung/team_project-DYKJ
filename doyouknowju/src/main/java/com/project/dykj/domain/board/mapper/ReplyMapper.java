package com.project.dykj.domain.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.project.dykj.domain.board.model.vo.Reply;

@Mapper
public interface ReplyMapper {

	int insertReply(Reply reply);

	List<Reply> selectReplies(@Param("boardId") long boardId);

	int softDeleteReply(@Param("replyId") long replyId);
}
