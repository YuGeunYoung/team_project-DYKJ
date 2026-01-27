package com.project.dykj.domain.notification.dto;

import lombok.Data;
import java.util.Date;

@Data
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