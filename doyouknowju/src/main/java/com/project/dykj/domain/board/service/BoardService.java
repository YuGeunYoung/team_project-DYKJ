package com.project.dykj.domain.board.service;

import java.util.List;

import com.project.dykj.domain.board.model.vo.Board;
import com.project.dykj.domain.board.model.vo.Reply;

public interface BoardService {

    long createPost(Board board);

    Board getPost(long postId, boolean incrementView);

    List<Board> listPosts(String boardType, String stockId, String condition, String keyword, int page, int size);

    void updatePost(long postId, Board board);

    void deletePost(long postId);

    long addReply(long postId, Reply reply);

    List<Reply> listReplies(long postId);

    List<Board> listPostsByUserId(String userId, int page, int size);

    List<Reply> listRepliesByUserId(String userId, int page, int size);

    void deleteReply(long replyId);

    void updateReply(long replyId, Reply reply);

    /**
     * 메인 페이지 인기글 조회
     * @param boardType free|stock (자유/종목 게시판 구분)
     * @param range realtime|weekly (기간 구분)
     * @param limit 상위 N개
     */
    List<Board> popularityBoard(String boardType, String range, int limit);
}
