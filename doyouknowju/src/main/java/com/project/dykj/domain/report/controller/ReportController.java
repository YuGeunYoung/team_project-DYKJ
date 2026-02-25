package com.project.dykj.domain.report.controller;

import com.project.dykj.domain.report.service.ReportService;
import com.project.dykj.domain.report.vo.ReportVo;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    
    // 리액트와 통신할 API
    @PostMapping("/insert")
    public String insertReport(@RequestBody ReportVo reportVo) {
        boolean success = reportService.registReport(reportVo);
        return success ? "success" : "fail";
    }
    
    // [taek] 신고 내역 조회
    @GetMapping("/list")
    public ResponseEntity<?> getReportList(
    		@RequestParam(defaultValue = "1") int page,
    		@RequestParam(defaultValue = "10") int size,
    		@RequestParam(required = false) String status) {
    	return ResponseEntity.ok(reportService.getReportListPaged(page, size, status));
    }
    
    // 신고 단건 조회
    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReportById(@PathVariable long reportId) {
        ReportVo report = reportService.getReportById(reportId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    // 신고 상태 변경
    @PutMapping("/{reportId}/status")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable long reportId,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        Boolean shouldHide = (Boolean) body.get("shouldHide");
        if (shouldHide == null)
            shouldHide = false;

        boolean result = reportService.updateReportStatus(reportId, status, shouldHide);
        return result ? ResponseEntity.ok("STATUS_UPDATED") : ResponseEntity.badRequest().body("FAIL");
    }
}