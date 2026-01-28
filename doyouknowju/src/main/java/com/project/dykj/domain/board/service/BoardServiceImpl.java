package com.project.dykj.domain.board.service;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.board.dao.BoardDao;
import com.project.dykj.domain.board.model.vo.Board;
import com.project.dykj.domain.board.model.vo.Reply;

@Service
public class BoardServiceImpl implements BoardService {

    @Autowired
    private BoardDao dao;

    @Autowired
    private SqlSessionTemplate sqlSession;

    public BoardServiceImpl(BoardDao dao, SqlSessionTemplate sqlSession) {
        this.dao = dao;
        this.sqlSession = sqlSession;
    }

    @Transactional
    @Override
    public long createPost(Board board) {
        validateCreate(board);
        dao.insertPost(sqlSession, board);
        return board.getBoardId();
    }

    @Transactional
    @Override
    public Board getPost(long postId, boolean incrementView) {
        if (incrementView) {
            dao.incrementViewCnt(sqlSession, postId);
        }
        Board board = dao.selectPostDetail(sqlSession, postId);
        if (board == null) {
            throw new IllegalArgumentException("post not found");
        }
        return board;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Board> listPosts(String boardType, String stockId, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        int offset = (safePage - 1) * safeSize;
        return dao.selectPostList(sqlSession, boardType, stockId, offset, safeSize);
    }

    @Transactional
    @Override
    public void updatePost(long postId, Board board) {
        if (board == null) {
            throw new IllegalArgumentException("body is required");
        }
        if (isBlank(board.getBoardTitle()) || isBlank(board.getBoardContent())) {
            throw new IllegalArgumentException("title/content are required");
        }
        board.setBoardId((int) postId);
        int updated = dao.updatePost(sqlSession, board);
        if (updated == 0) {
            throw new IllegalArgumentException("post not found");
        }
    }

    @Transactional
    @Override
    public void deletePost(long postId) {
        int updated = dao.softDeletePost(sqlSession, postId);
        if (updated == 0) {
            throw new IllegalArgumentException("post not found");
        }
    }

    @Transactional
    @Override
    public long addReply(long postId, Reply reply) {
        if (reply == null || isBlank(reply.getUserId()) || isBlank(reply.getReplyContent())) {
            throw new IllegalArgumentException("userId/content are required");
        }
        if (dao.selectPostDetail(sqlSession, postId) == null) {
            throw new IllegalArgumentException("post not found");
        }

        reply.setBoardId((int) postId);
        dao.insertReply(sqlSession, reply);
        return reply.getReplyId();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Reply> listReplies(long postId) {
        return dao.selectReplies(sqlSession, postId);
    }

    @Transactional
    @Override
    public void deleteReply(long replyId) {
        int updated = dao.softDeleteReply(sqlSession, replyId);
        if (updated == 0) {
            throw new IllegalArgumentException("comment not found");
        }
    }

    private void validateCreate(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("body is required");
        }
        if (isBlank(board.getBoardType())
                || isBlank(board.getUserId())
                || isBlank(board.getBoardTitle())
                || isBlank(board.getBoardContent())) {
            throw new IllegalArgumentException("boardType/userId/title/content are required");
        }
        if ("STOCK".equalsIgnoreCase(board.getBoardType()) && isBlank(board.getStockId())) {
            throw new IllegalArgumentException("stockId is required for STOCK board");
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}

