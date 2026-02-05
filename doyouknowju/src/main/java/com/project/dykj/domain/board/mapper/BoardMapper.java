package com.project.dykj.domain.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.project.dykj.domain.board.model.vo.Board;

@Mapper
public interface BoardMapper {

	int insertPost(Board board);

	int incrementViewCnt(@Param("boardId") long boardId);

	Board selectPostDetail(@Param("boardId") long boardId);

	List<Board> selectPostList(
			@Param("boardType") String boardType,
			@Param("stockId") String stockId,
			@Param("condition") String condition,
			@Param("keyword") String keyword,
			@Param("offset") int offset,
			@Param("size") int size
	);

	List<Board> selectPostListByUserId(
			@Param("userId") String userId,
			@Param("offset") int offset,
			@Param("size") int size
	);

	int updatePost(Board board);

	int softDeletePost(@Param("boardId") long boardId);
}
