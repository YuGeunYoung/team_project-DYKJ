package com.project.dykj.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.project.dykj.board.model.vo.BoardPostCreateRequest;
import com.project.dykj.board.model.vo.BoardPostDetail;
import com.project.dykj.board.model.vo.BoardPostListItem;
import com.project.dykj.board.model.vo.BoardPostUpdateRequest;

@Mapper
public interface BoardMapper {

	long nextPostId();

	int insertPost(@Param("postId") long postId, @Param("req") BoardPostCreateRequest req);

	int incrementViewCnt(@Param("postId") long postId);

	BoardPostDetail selectPostDetail(@Param("postId") long postId);

	List<BoardPostListItem> selectPostList(
			@Param("boardType") String boardType,
			@Param("stockId") String stockId,
			@Param("offset") int offset,
			@Param("size") int size
	);

	int updatePost(@Param("postId") long postId, @Param("req") BoardPostUpdateRequest req);

	int softDeletePost(@Param("postId") long postId);
}
