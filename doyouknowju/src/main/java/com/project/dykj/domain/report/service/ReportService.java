package com.project.dykj.domain.report.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.board.mapper.BoardMapper;
import com.project.dykj.domain.board.mapper.ReplyMapper;
import com.project.dykj.domain.board.model.vo.Board;
import com.project.dykj.domain.board.model.vo.Reply;
import com.project.dykj.domain.chat.dto.ChatMessageVO;
import com.project.dykj.domain.chat.repository.ChatRepository;
import com.project.dykj.domain.report.mapper.ReportMapper;
import com.project.dykj.domain.report.vo.ReportVo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportMapper reportMapper;
    private final BoardMapper boardMapper;
    private final ReplyMapper replyMapper;
    private final ChatRepository chatRepository;
    
    //비즈니스로직 처리
    @Transactional
    public boolean registReport(ReportVo reportVo) {
        return reportMapper.insertReport(reportVo) > 0;
    }
    
    // [taek] 신고 목록 조회
	public List<ReportVo> getReportList() {
		return reportMapper.selectReportList();
	}

	public ReportVo getReportById(long reportId) {
		ReportVo report = reportMapper.selectReportById(reportId);
		if (report != null) {
			// [taek] 신고된 실제 내용 조회
			if ("BOARD".equals(report.getReportType())) {
				Board board = boardMapper.selectPostDetail(report.getContentId());
				if (board != null)
					report.setContent(board.getBoardContent());
			} else if ("REPLY".equals(report.getReportType())) {
				Reply reply = replyMapper.selectReplyById(report.getContentId());
				if (reply != null)
					report.setContent(reply.getReplyContent());
			} else if ("CHAT".equals(report.getReportType())) {
				ChatMessageVO chat = chatRepository.selectChatMessageById(report.getContentId());
				if (chat != null)
					report.setContent(chat.getChatContent());
			}
		}
		return report;
	}

	@Transactional
	public boolean updateReportStatus(long reportId, String status) {
		return reportMapper.updateReportStatus(reportId, status) > 0;
	}

	public java.util.Map<String, Object> getReportListPaged(int page, int size, String status) {
		int offset = (page - 1) * size;
		java.util.Map<String, Object> params = new java.util.HashMap<>();
		params.put("offset", offset);
		params.put("size", size);
		params.put("status", status);

		java.util.List<ReportVo> reports = reportMapper.selectReportListPaged(params);
		int total = reportMapper.selectTotalReportCount(params);

		// [taek] 상태별 카운트 추가
		java.util.Map<String, Object> pendingParams = new java.util.HashMap<>();
		pendingParams.put("status", "PENDING");
		int pendingCount = reportMapper.selectTotalReportCount(pendingParams);

		java.util.Map<String, Object> processedParams = new java.util.HashMap<>();
		processedParams.put("status", "PROCESSED");
		int processedCount = reportMapper.selectTotalReportCount(processedParams);

		java.util.Map<String, Object> result = new java.util.HashMap<>();
		result.put("reports", reports);
		result.put("total", total);
		result.put("pendingCount", pendingCount);
		result.put("processedCount", processedCount);
		result.put("page", page);
		result.put("size", size);
		return result;
	}
}