package com.project.dykj.domain.report.mapper;

//Report 인터페이스 공간
import com.project.dykj.domain.report.vo.ReportVo;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
    int insertReport(ReportVo reportVo);

	List<ReportVo> selectReportList();

	ReportVo selectReportById(long reportId);

	int updateReportStatus(long reportId, String status);

	List<ReportVo> selectReportListPaged(Map<String, Object> params);

	int selectTotalReportCount(Map<String, Object> params);
    
}