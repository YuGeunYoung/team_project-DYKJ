package com.project.dykj.domain.notification.service;

import java.util.List;
import com.project.dykj.domain.notification.dto.NotificationVO;

public interface NotificationService {
    // [수정] 페이징 처리를 위해 offset과 size를 인자로 추가
    List<NotificationVO> getNotificationList(String userId, int offset, int size);

    void markAsRead(Long notiNo);

    void createNotification(NotificationVO notification);
}