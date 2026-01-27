package com.project.dykj.board.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.project.dykj.board.model.vo.Board;
import com.project.dykj.board.model.vo.Reply;

@Repository
public class BoardDao {

    private static final String NS_BOARD = "boardMapper.";
    private static final String NS_REPLY = "replyMapper.";

    public int insertPost(SqlSessionTemplate sqlSession, Board board) {
        return sqlSession.insert(NS_BOARD + "insertPost", board);
    }

    public int incrementViewCnt(SqlSessionTemplate sqlSession, long postId) {
        return sqlSession.update(NS_BOARD + "incrementViewCnt", Map.of("postId", postId));
    }

    public Board selectPostDetail(SqlSessionTemplate sqlSession, long postId) {
        return sqlSession.selectOne(NS_BOARD + "selectPostDetail", Map.of("postId", postId));
    }

    public List<Board> selectPostList(
            SqlSessionTemplate sqlSession,
            String boardType,
            String stockId,
            int offset,
            int size
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("boardType", boardType);
        params.put("stockId", stockId);
        params.put("offset", offset);
        params.put("size", size);
        return sqlSession.selectList(NS_BOARD + "selectPostList", params);
    }

    public int updatePost(SqlSessionTemplate sqlSession, Board board) {
        return sqlSession.update(NS_BOARD + "updatePost", board);
    }

    public int softDeletePost(SqlSessionTemplate sqlSession, long postId) {
        return sqlSession.update(NS_BOARD + "softDeletePost", Map.of("postId", postId));
    }

    public int insertReply(SqlSessionTemplate sqlSession, Reply reply) {
        return sqlSession.insert(NS_REPLY + "insertReply", reply);
    }

    public List<Reply> selectReplies(SqlSessionTemplate sqlSession, long postId) {
        return sqlSession.selectList(NS_REPLY + "selectReplies", Map.of("postId", postId));
    }

    public int softDeleteReply(SqlSessionTemplate sqlSession, long replyId) {
        return sqlSession.update(NS_REPLY + "softDeleteReply", Map.of("replyId", replyId));
    }
}
