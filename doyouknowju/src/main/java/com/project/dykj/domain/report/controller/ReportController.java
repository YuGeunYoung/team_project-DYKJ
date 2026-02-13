package com.project.dykj.domain.report.controller;

import com.project.dykj.domain.report.service.ReportService;
import com.project.dykj.domain.report.vo.ReportVo;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
}