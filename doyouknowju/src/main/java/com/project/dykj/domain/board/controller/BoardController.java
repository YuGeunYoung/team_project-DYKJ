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
    @PostMapping("/insert")
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody Board board) {
        long boardId = boardService.createPost(board);
        return ResponseEntity.created(URI.create("/api/boards/detail/" + boardId))
                .body(Map.of("boardId", boardId));
    }

    /** 게시글 목록 조회 (페이지네이션) */
    @GetMapping("/list")
    public List<Board> listPosts(
            @RequestParam(required = false) String boardType,
            @RequestParam(required = false) String stockId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return boardService.listPosts(boardType, stockId, page, size);
    }

    /** 게시글 상세 조회 (view=true면 조회수 증가) */
    @GetMapping( "/detail/{boardId}")
    public Board getPost(
            @PathVariable long boardId,
            @RequestParam(defaultValue = "true") boolean view
    ) {
        return boardService.getPost(boardId, view);
    }

    /** 게시글 수정 */
    @PutMapping("/update/{boardId}")
    public ResponseEntity<Void> updatePost(@PathVariable long boardId, @RequestBody Board board) {
        boardService.updatePost(boardId, board);
        return ResponseEntity.noContent().build();
    }

    /** 게시글 삭제(소프트 삭제) */
    @DeleteMapping( "/delete/{boardId}")
    public ResponseEntity<Void> deletePost(@PathVariable long boardId) {
        boardService.deletePost(boardId);
        return ResponseEntity.noContent().build();
    }

    /** 댓글 작성 */
    @PostMapping("/{boardId}/replies/insert")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable long boardId,
            @RequestBody Reply reply
    ) {
        long replyId = boardService.addReply(boardId, reply);
        return ResponseEntity.created(URI.create("/api/boards/replies/" + replyId))
                .body(Map.of("replyId", replyId));
    }

    /** 댓글 목록 조회 */
    @GetMapping("/{boardId}/replies")
    public List<Reply> listComments(@PathVariable long boardId) {
        return boardService.listReplies(boardId);
    }

    /** 댓글 삭제(소프트 삭제) */
    @DeleteMapping("/replies/{replyId}/delete")
    public ResponseEntity<Void> deleteComment(@PathVariable long replyId) {
        boardService.deleteReply(replyId);
        return ResponseEntity.noContent().build();
    }

    /** 댓글 수정 */
    @PutMapping("/replies/{replyId}/update")
    public ResponseEntity<Void> updateComment(
            @PathVariable long replyId,
            @RequestBody Reply reply
    ) {
        boardService.updateReply(replyId, reply);
        return ResponseEntity.noContent().build();
    }
}
