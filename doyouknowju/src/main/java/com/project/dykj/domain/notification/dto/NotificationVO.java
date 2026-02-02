package com.project.dykj.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Builder // 이 부분이 추가되어야 .builder()를 쓸 수 있습니다!
@NoArgsConstructor // 기본 생성자
@AllArgsConstructor // 전체 인자 생성자
public class NotificationVO {
    private Long notiNo;
    private String userId;
    private String notiType;
    private String notiChannel;
    private String message;
    private String notiUrl;
    private String isRead;
    private Date createAt;
    private String sendStatus;
    private Date sendAt;
    private String errorMsg;
    private Integer retryCount;
}