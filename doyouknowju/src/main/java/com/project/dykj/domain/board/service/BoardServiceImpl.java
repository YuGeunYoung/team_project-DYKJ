package com.project.dykj.domain.board.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.board.mapper.BoardMapper;
import com.project.dykj.domain.board.mapper.ReplyMapper;
import com.project.dykj.domain.board.model.vo.Board;
import com.project.dykj.domain.board.model.vo.Reply;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;
    private final ReplyMapper replyMapper;
    private final BadWordFilterService badWordFilterService;

    // BoardMapper / ReplyMapper로 DB 접근을 수행
    public BoardServiceImpl(
            BoardMapper boardMapper,
            ReplyMapper replyMapper,
            BadWordFilterService badWordFilterService
    ) {
        this.boardMapper = boardMapper;
        this.replyMapper = replyMapper;
        this.badWordFilterService = badWordFilterService;
    }

    // 게시글 등록
    // - 필수값 검증(게시판 타입/작성자/제목/내용)
    // - STOCK 게시판이면 stockId 필수
    // - FREE 게시판이면 stockId는 항상 null로 저장
    @Transactional
    @Override
    public long createPost(Board board) {
        validateCreate(board);
        board.setBoardTitle(badWordFilterService.mask(board.getBoardTitle()));
        board.setBoardContent(badWordFilterService.mask(board.getBoardContent()));
        boardMapper.insertPost(board);
        return board.getBoardId();
    }

    // 게시글 상세 조회
    // - view=true면 조회수 1 증가
    // - 존재하지 않으면 예외
    @Transactional
    @Override
    public Board getPost(long postId, boolean incrementView) {
        if (incrementView) {
            boardMapper.incrementViewCnt(postId);
        }
        Board board = boardMapper.selectPostDetail(postId);
        if (board == null) {
            throw new IllegalArgumentException("post not found");
        }
        return board;
    }

    // 게시글 목록 조회(페이징)
    // - page/size를 안전하게 보정(page>=1, 1<=size<=50)
    // - boardType은 FREE/STOCK만 허용(그 외는 전체 조회로 처리)
    // - FREE면 stockId 필터는 강제로 무시(null)하여 "종목코드 없는 글"만 내려오도록 한다.
    @Transactional(readOnly = true)
    @Override
    public List<Board> listPosts(String boardType, String stockId, String condition, String keyword, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        int offset = (safePage - 1) * safeSize;
        String normalizedBoardType = normalizeBoardType(boardType);
        String normalizedStockId = (normalizedBoardType != null && "FREE".equals(normalizedBoardType))
                ? null
                : blankToNull(stockId);

        String normalizedCondition = normalizeCondition(condition);
        String normalizedKeyword = blankToNull(keyword);

        return boardMapper.selectPostList(
                normalizedBoardType,
                normalizedStockId,
                normalizedCondition,
                normalizedKeyword,
                offset,
                safeSize
        );
    }

    // 게시글 수정
    // - 제목/내용 필수
    // - UPDATE 결과가 0이면(없거나 삭제됨) 예외
    @Transactional
    @Override
    public void updatePost(long postId, Board board) {
        if (board == null) {
            throw new IllegalArgumentException("body is required");
        }
        if (isBlank(board.getBoardTitle()) || isBlank(board.getBoardContent())) {
            throw new IllegalArgumentException("title/content are required");
        }

        // 기존 게시글을 조회해서, boardType/stockId를 변경할 때 정합성을 보장한다.
        Board existing = boardMapper.selectPostDetail(postId);
        if (existing == null) {
            throw new IllegalArgumentException("post not found");
        }

        String nextBoardType = normalizeBoardType(board.getBoardType());
        if (nextBoardType == null) {
            nextBoardType = normalizeBoardType(existing.getBoardType());
        }
        if (nextBoardType == null) {
            throw new IllegalArgumentException("boardType is required");
        }

        String nextStockId = blankToNull(board.getStockId());

        // FREE <-> STOCK 전환 규칙
        if ("FREE".equals(nextBoardType)) {
            nextStockId = null;
        } else if ("STOCK".equals(nextBoardType)) {
            if (nextStockId == null) {
                throw new IllegalArgumentException("stockId is required for STOCK board");
            }
        }

        board.setBoardId((int) postId);
        board.setBoardType(nextBoardType);
        board.setStockId(nextStockId);
        board.setBoardTitle(badWordFilterService.mask(board.getBoardTitle()));
        board.setBoardContent(badWordFilterService.mask(board.getBoardContent()));

        int updated = boardMapper.updatePost(board);
        if (updated == 0) {
            throw new IllegalArgumentException("post not found");
        }
    }

    // 게시글 삭제(소프트 삭제)
    // - DELETE_DATE를 현재시간으로 업데이트
    @Transactional
    @Override
    public void deletePost(long postId) {
        int updated = boardMapper.softDeletePost(postId);
        if (updated == 0) {
            throw new IllegalArgumentException("post not found");
        }
    }

    // 댓글 등록
    // - 필수값 검증(userId, replyContent)
    // - 대상 게시글 존재 여부 확인
    @Transactional
    @Override
    public long addReply(long postId, Reply reply) {
        if (reply == null || isBlank(reply.getUserId()) || isBlank(reply.getReplyContent())) {
            throw new IllegalArgumentException("userId/content are required");
        }
        if (boardMapper.selectPostDetail(postId) == null) {
            throw new IllegalArgumentException("post not found");
        }

        reply.setBoardId((int) postId);
        reply.setReplyContent(badWordFilterService.mask(reply.getReplyContent()));
        replyMapper.insertReply(reply);
        return reply.getReplyId();
    }

    // 댓글 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<Reply> listReplies(long postId) {
        return replyMapper.selectReplies(postId);
    }

    // 댓글 삭제(소프트 삭제)
    @Transactional(readOnly = true)
    @Override
    public List<Board> listPostsByUserId(String userId, int page, int size) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        int offset = (safePage - 1) * safeSize;
        return boardMapper.selectPostListByUserId(userId.trim(), offset, safeSize);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Reply> listRepliesByUserId(String userId, int page, int size) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        int offset = (safePage - 1) * safeSize;
        return replyMapper.selectRepliesByUserId(userId.trim(), offset, safeSize);
    }

    @Transactional
    @Override
    public void deleteReply(long replyId) {
        int updated = replyMapper.softDeleteReply(replyId);
        if (updated == 0) {
            throw new IllegalArgumentException("comment not found");
        }
    }

    // 댓글 수정
    // - replyContent 필수
    // - UPDATE 결과가 0이면(없거나 삭제됨) 예외
    @Transactional
    @Override
    public void updateReply(long replyId, Reply reply) {
        if (reply == null || isBlank(reply.getReplyContent())) {
            throw new IllegalArgumentException("content is required");
        }
        reply.setReplyId((int) replyId);
        reply.setReplyContent(badWordFilterService.mask(reply.getReplyContent()));
        int updated = replyMapper.updateReply(reply);
        if (updated == 0) {
            throw new IllegalArgumentException("comment not found");
        }
    }

    /**
     * 메인 페이지 인기글(실시간/주간) 조회
     * - fromDate(기간 시작)는 서비스에서 계산 후 Mapper로 전달
     * - 실제 SQL은 board-mapper.xml의 popularityBoard에서 구현
     */
    @Transactional(readOnly = true)
    @Override
    public List<Board> popularityBoard(String range, int limit) {
        String normalizedRange = isBlank(range) ? "realtime" : range.trim().toLowerCase();
        int days = switch (normalizedRange) {
            case "weekly" -> 7;
            case "realtime" -> 1;
            default -> throw new IllegalArgumentException("range must be realtime|weekly");
        };

        int safeLimit = Math.min(5, Math.max(1, limit));

        LocalDateTime from = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        Instant instant = from.atZone(ZoneId.systemDefault()).toInstant();
        Date fromDate = Date.from(instant);

        return boardMapper.popularityBoard(fromDate, safeLimit);
    }

    // 게시글 등록 시 입력값 검증 및 정규화
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
        if ("FREE".equalsIgnoreCase(board.getBoardType())) {
            board.setStockId(null);
        }
    }

    // null/공백 문자열 체크 유틸
    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    // 공백이면 null, 아니면 trim
    private static String blankToNull(String v) {
        return isBlank(v) ? null : v.trim();
    }

    // boardType 파라미터를 FREE/STOCK으로만 정규화(그 외는 null 반환 -> 전체 조회)
    private static String normalizeBoardType(String boardType) {
        if (isBlank(boardType)) {
            return null;
        }
        String upper = boardType.trim().toUpperCase();
        return ("FREE".equals(upper) || "STOCK".equals(upper)) ? upper : null;
    }

    // 검색 조건 정규화: title | content | writer 만 허용 (그 외는 null -> 검색 미적용)
    private static String normalizeCondition(String condition) {
        if (isBlank(condition)) {
            return null;
        }
        String lower = condition.trim().toLowerCase();
        return (lower.equals("title") || lower.equals("content") || lower.equals("writer")) ? lower : null;
    }
}
