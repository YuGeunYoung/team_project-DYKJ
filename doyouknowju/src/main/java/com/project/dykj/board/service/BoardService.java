package com.project.dykj.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.board.model.vo.BoardCommentCreateRequest;
import com.project.dykj.board.model.vo.BoardCommentItem;
import com.project.dykj.board.model.vo.BoardPostCreateRequest;
import com.project.dykj.board.model.vo.BoardPostDetail;
import com.project.dykj.board.model.vo.BoardPostListItem;
import com.project.dykj.board.model.vo.BoardPostUpdateRequest;
import com.project.dykj.board.mapper.BoardMapper;
import com.project.dykj.board.mapper.ReplyMapper;

@Service
public class BoardService {

	private final BoardMapper boardMapper;
	private final ReplyMapper replyMapper;

	public BoardService(BoardMapper boardMapper, ReplyMapper replyMapper) {
		this.boardMapper = boardMapper;
		this.replyMapper = replyMapper;
	}

	/**
	 * 게시글 생성 (시퀀스로 ID 발급 후 INSERT)
	 */
	@Transactional
	public long createPost(BoardPostCreateRequest req) {
		validateCreate(req);
		long postId = boardMapper.nextPostId();
		boardMapper.insertPost(postId, req);
		return postId;
	}

	/**
	 * 게시글 상세 조회 (필요 시 조회수 증가)
	 */
	@Transactional
	public BoardPostDetail getPost(long postId, boolean incrementView) {
		if (incrementView) {
			boardMapper.incrementViewCnt(postId);
		}
		BoardPostDetail detail = boardMapper.selectPostDetail(postId);
		if (detail == null) {
			throw new IllegalArgumentException("post not found");
		}
		return detail;
	}

	/**
	 * 게시글 목록 조회 (boardType/stockId 필터 + 페이지네이션)
	 */
	@Transactional(readOnly = true)
	public List<BoardPostListItem> listPosts(String boardType, String stockId, int page, int size) {
		int safePage = Math.max(1, page);
		int safeSize = Math.min(50, Math.max(1, size));
		int offset = (safePage - 1) * safeSize;
		return boardMapper.selectPostList(boardType, stockId, offset, safeSize);
	}

	/**
	 * 게시글 수정
	 */
	@Transactional
	public void updatePost(long postId, BoardPostUpdateRequest req) {
		if (req == null) {
			throw new IllegalArgumentException("body is required");
		}
		if (isBlank(req.getTitle()) || isBlank(req.getContent())) {
			throw new IllegalArgumentException("title/content are required");
		}
		int updated = boardMapper.updatePost(postId, req);
		if (updated == 0) {
			throw new IllegalArgumentException("post not found");
		}
	}

	/**
	 * 게시글 삭제(소프트 삭제)
	 */
	@Transactional
	public void deletePost(long postId) {
		int updated = boardMapper.softDeletePost(postId);
		if (updated == 0) {
			throw new IllegalArgumentException("post not found");
		}
	}

	/**
	 * 댓글 작성
	 */
	@Transactional
	public long addComment(long postId, BoardCommentCreateRequest req) {
		if (req == null || isBlank(req.getUserId()) || isBlank(req.getContent())) {
			throw new IllegalArgumentException("userId/content are required");
		}
		if (boardMapper.selectPostDetail(postId) == null) {
			throw new IllegalArgumentException("post not found");
		}

		long commentId = replyMapper.nextCommentId();
		replyMapper.insertComment(commentId, postId, req);
		return commentId;
	}

	/**
	 * 댓글 목록 조회
	 */
	@Transactional(readOnly = true)
	public List<BoardCommentItem> listComments(long postId) {
		return replyMapper.selectComments(postId);
	}

	/**
	 * 댓글 삭제(소프트 삭제)
	 */
	@Transactional
	public void deleteComment(long commentId) {
		int updated = replyMapper.softDeleteComment(commentId);
		if (updated == 0) {
			throw new IllegalArgumentException("comment not found");
		}
	}

	private void validateCreate(BoardPostCreateRequest req) {
		if (req == null) {
			throw new IllegalArgumentException("body is required");
		}
		if (isBlank(req.getBoardType()) || isBlank(req.getUserId()) || isBlank(req.getTitle()) || isBlank(req.getContent())) {
			throw new IllegalArgumentException("boardType/userId/title/content are required");
		}
		if ("STOCK".equalsIgnoreCase(req.getBoardType()) && isBlank(req.getStockId())) {
			throw new IllegalArgumentException("stockId is required for STOCK board");
		}
	}

	private static boolean isBlank(String v) {
		return v == null || v.trim().isEmpty();
	}
}
