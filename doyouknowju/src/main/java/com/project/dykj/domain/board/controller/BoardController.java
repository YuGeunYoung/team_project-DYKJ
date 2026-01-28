package com.project.dykj.domain.board.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.board.model.vo.Board;
import com.project.dykj.domain.board.model.vo.Reply;
import com.project.dykj.domain.board.service.BoardService;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** 게시글 생성 (STOCK/FREE) */
    @PostMapping("/posts")
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody Board board) {
        long postId = boardService.createPost(board);
        return ResponseEntity.created(URI.create("/api/boards/posts/" + postId))
                .body(Map.of("postId", postId));
    }

    /** 게시글 목록 조회 (페이지네이션) */
    @GetMapping("/posts")
    public List<Board> listPosts(
            @RequestParam(required = false) String boardType,
            @RequestParam(required = false) String stockId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return boardService.listPosts(boardType, stockId, page, size);
    }

    /** 게시글 상세 조회 (view=true면 조회수 증가) */
    @GetMapping("/posts/{postId}")
    public Board getPost(
            @PathVariable long postId,
            @RequestParam(defaultValue = "true") boolean view
    ) {
        return boardService.getPost(postId, view);
    }

    /** 게시글 수정 */
    @PutMapping("/posts/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable long postId, @RequestBody Board board) {
        boardService.updatePost(postId, board);
        return ResponseEntity.noContent().build();
    }

    /** 게시글 삭제(소프트 삭제) */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable long postId) {
        boardService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    /** 댓글 작성 */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable long postId,
            @RequestBody Reply reply
    ) {
        long replyId = boardService.addReply(postId, reply);
        return ResponseEntity.created(URI.create("/api/boards/comments/" + replyId))
                .body(Map.of("replyId", replyId));
    }

    /** 댓글 목록 조회 */
    @GetMapping("/posts/{postId}/comments")
    public List<Reply> listComments(@PathVariable long postId) {
        return boardService.listReplies(postId);
    }

    /** 댓글 삭제(소프트 삭제) */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable long commentId) {
        boardService.deleteReply(commentId);
        return ResponseEntity.noContent().build();
    }
}

