package com.project.dykj.domain.report.mapper;

//Report 인터페이스 공간
import com.project.dykj.domain.report.vo.ReportVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
    int insertReport(ReportVo reportVo);
    
    
    
}