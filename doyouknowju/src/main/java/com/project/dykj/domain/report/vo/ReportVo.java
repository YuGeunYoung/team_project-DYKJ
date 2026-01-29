package com.project.dykj.domain.report.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportVo {
    private long reportId;       // PK (시퀀스)
    private String reportType;   // 'BOARD', 'REPLY', 'CHAT'
    private long contentId;      // 신고 대상 번호
    private String reporterId;   // 신고자 아이디
    private String targetId;     // 피신고자 아이디
    private String reportReason; // 신고 사유 (태그 값)
    private Date reportDate;     // 신고 일시
    private String status;       // 처리 상태 (PENDING, DONE, REJECT)
}