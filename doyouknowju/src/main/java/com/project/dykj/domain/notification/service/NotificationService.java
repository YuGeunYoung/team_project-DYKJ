package com.project.dykj.domain.notification.service;

import java.util.List;
import com.project.dykj.domain.notification.dto.NotificationVO;

public interface NotificationService {
    // [설명] 유저의 알림 목록을 가져오는 기능 정의
    List<NotificationVO> getNotificationList(String userId);

    // [설명] 알림을 읽음 처리하는 기능 정의
    void markAsRead(Long notiNo);

    // [설명] 새 알림을 생성하는 기능 정의 (주식 등락폭 로직 등에서 사용 예정)
    void createNotification(NotificationVO notification);
}
