package com.project.dykj.domain.report.service;

import com.project.dykj.domain.report.mapper.ReportMapper;
import com.project.dykj.domain.report.vo.ReportVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportMapper reportMapper;
//비즈니스로직 처리
    @Transactional
    public boolean registReport(ReportVo reportVo) {
        return reportMapper.insertReport(reportVo) > 0;
    }
}