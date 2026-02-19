package com.project.dykj.domain.report.controller;

import com.project.dykj.domain.report.service.ReportService;
import com.project.dykj.domain.report.vo.ReportVo;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    
    //리액트와 통신할 API
    @PostMapping("/insert")
    public String insertReport(@RequestBody ReportVo reportVo) {
        boolean success = reportService.registReport(reportVo);
        return success ? "success" : "fail";
    }
    
    // [taek] 신고 내역 조회
    @GetMapping("/list")
    public List<ReportVo> getReportList() {
    	return reportService.getReportList();
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
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        boolean result = reportService.updateReportStatus(reportId, status);
        return result ? ResponseEntity.ok("STATUS_UPDATED") : ResponseEntity.badRequest().body("FAIL");
    }
}