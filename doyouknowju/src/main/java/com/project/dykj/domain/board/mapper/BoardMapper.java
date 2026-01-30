package com.project.dykj.domain.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.project.dykj.domain.board.model.vo.Board;

@Mapper
public interface BoardMapper {

	int insertPost(Board board);

	int incrementViewCnt(@Param("postId") long postId);

	Board selectPostDetail(@Param("postId") long postId);

	List<Board> selectPostList(
			@Param("boardType") String boardType,
			@Param("stockId") String stockId,
			@Param("offset") int offset,
			@Param("size") int size
	);

	int updatePost(Board board);

	int softDeletePost(@Param("postId") long postId);
}

