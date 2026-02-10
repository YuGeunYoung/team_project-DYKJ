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
import com.project.dykj.domain.game.service.GameService;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.mapper.MemberMapper;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;
    private final ReplyMapper replyMapper;
    private final BadWordFilterService badWordFilterService;
    private final MemberMapper memberMapper;
    private final GameService gameService; //[taek] 도전과제 달성 확인용

    // Board/Reply Mapper를 통한 DB 접근
    public BoardServiceImpl(
            BoardMapper boardMapper,
            ReplyMapper replyMapper,
            BadWordFilterService badWordFilterService,
            GameService gameService,
            MemberMapper memberMapper
    ) {
        this.boardMapper = boardMapper;
        this.replyMapper = replyMapper;
        this.badWordFilterService = badWordFilterService;
        this.gameService = gameService;
        this.memberMapper = memberMapper;
    }

    // 게시글 등록
    // - 필수값 검증: boardType, userId, title, content
    // - STOCK 게시판이면 stockId 필수
    // - FREE 게시판이면 stockId 강제 null 처리
    @Transactional
    @Override
    public long createPost(Board board) {
        validateCreate(board);
        validateMemberWriteEligibility(board.getUserId());
        board.setBoardTitle(badWordFilterService.mask(board.getBoardTitle()));
        board.setBoardContent(badWordFilterService.mask(board.getBoardContent()));
        boardMapper.insertPost(board);
        
        // [taek] 도전과제 6번 달성 확인
        if(board.getBoardType().equals("FREE")) {
        	gameService.recordAchievement(board.getUserId(), 6);
        }
        
        // [taek] 게시글 작성 횟수 도전과제 확인
        gameService.checkBoardAchievements(board.getUserId());
        
        return board.getBoardId();
    }

    // 게시글 상세 조회
    // - incrementView=true면 조회수 증가
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

    // 게시글 목록 조회(페이지네이션 + 조건/키워드 검색)
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
    // - FREE/STOCK 전환 규칙 검증
    @Transactional
    @Override
    public void updatePost(long postId, Board board) {
        if (board == null) {
            throw new IllegalArgumentException("body is required");
        }
        if (isBlank(board.getBoardTitle()) || isBlank(board.getBoardContent())) {
            throw new IllegalArgumentException("title/content are required");
        }

        // 기존 게시글 조회 후 boardType/stockId 정합성 보장
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
    @Transactional
    @Override
    public void deletePost(long postId) {
        int updated = boardMapper.softDeletePost(postId);
        if (updated == 0) {
            throw new IllegalArgumentException("post not found");
        }
    }

    // 댓글 등록
    @Transactional
    @Override
    public long addReply(long postId, Reply reply) {
        if (reply == null || isBlank(reply.getUserId()) || isBlank(reply.getReplyContent())) {
            throw new IllegalArgumentException("userId/content are required");
        }
        validateMemberWriteEligibility(reply.getUserId());
        if (boardMapper.selectPostDetail(postId) == null) {
            throw new IllegalArgumentException("post not found");
        }

        reply.setBoardId((int) postId);
        reply.setReplyContent(badWordFilterService.mask(reply.getReplyContent()));
        replyMapper.insertReply(reply);
        
        //[taek] 도전과제 7번 달성 확인
        Board post = boardMapper.selectPostDetail(postId);
        if(!reply.getUserId().equals(post.getUserId())) {
        	gameService.recordAchievement(reply.getUserId(), 7);
        }
        
        return reply.getReplyId();
    }

    // 댓글 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<Reply> listReplies(long postId) {
        return replyMapper.selectReplies(postId);
    }

    // 사용자별 게시글 조회
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

    // 사용자별 댓글 조회
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

    // 댓글 삭제(소프트 삭제)
    @Transactional
    @Override
    public void deleteReply(long replyId) {
        int updated = replyMapper.softDeleteReply(replyId);
        if (updated == 0) {
            throw new IllegalArgumentException("comment not found");
        }
    }

    // 댓글 수정
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
     * 메인 페이지 인기글 조회
     * - range: realtime(1일), weekly(7일)
     * - limit: 1~5로 제한
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

    // 게시글 생성 입력값 검증
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

    // null/공백 문자열 체크
    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    // 공백이면 null, 아니면 trim
    private static String blankToNull(String v) {
        return isBlank(v) ? null : v.trim();
    }

    // boardType 정규화: FREE/STOCK만 허용
    private static String normalizeBoardType(String boardType) {
        if (isBlank(boardType)) {
            return null;
        }
        String upper = boardType.trim().toUpperCase();
        return ("FREE".equals(upper) || "STOCK".equals(upper)) ? upper : null;
    }

    // 검색 조건 정규화: title/content/writer만 허용
    private static String normalizeCondition(String condition) {
        if (isBlank(condition)) {
            return null;
        }
        String lower = condition.trim().toLowerCase();
        return (lower.equals("title") || lower.equals("content") || lower.equals("writer")) ? lower : null;
    }

    // 가입 7일 미만 회원의 게시글/댓글 작성을 차단
    private void validateMemberWriteEligibility(String userId) {
        Member member = memberMapper.findByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("invalid userId");
        }
        if (member.getEnrollDate() == null) {
            throw new IllegalArgumentException("member enrollDate is missing");
        }

        LocalDateTime availableAt = member.getEnrollDate().toLocalDate().plusDays(7).atStartOfDay();
        if (LocalDateTime.now().isBefore(availableAt)) {
            throw new IllegalArgumentException("가입 후 7일이 지나야 작성할 수 있습니다.");
        }
    }
}
