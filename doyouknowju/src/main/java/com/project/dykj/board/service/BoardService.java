package com.project.dykj.board.service;

import java.util.List;

import com.project.dykj.board.model.vo.Board;
import com.project.dykj.board.model.vo.Reply;

public interface BoardService {

	long createPost(Board board);

	Board getPost(long postId, boolean incrementView);

	List<Board> listPosts(String boardType, String stockId, int page, int size);

	void updatePost(long postId, Board board);

	void deletePost(long postId);

	long addReply(long postId, Reply reply);

	List<Reply> listReplies(long postId);

	void deleteReply(long replyId);
}
