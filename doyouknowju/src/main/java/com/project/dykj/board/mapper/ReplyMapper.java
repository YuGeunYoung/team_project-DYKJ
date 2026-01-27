package com.project.dykj.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.project.dykj.board.model.vo.BoardCommentCreateRequest;
import com.project.dykj.board.model.vo.BoardCommentItem;

@Mapper
public interface ReplyMapper {

	long nextCommentId();

	int insertComment(@Param("commentId") long commentId, @Param("postId") long postId, @Param("req") BoardCommentCreateRequest req);

	List<BoardCommentItem> selectComments(@Param("postId") long postId);

	int softDeleteComment(@Param("commentId") long commentId);
}
