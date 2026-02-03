package com.project.dykj.domain.notification.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.project.dykj.domain.notification.dto.NotificationVO;
import com.project.dykj.domain.notification.repository.NotificationRepository;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // [수정] Controller에서 받은 offset과 size를 Repository의 쿼리로 전달합니다.
    @Override
    public List<NotificationVO> getNotificationList(String userId, int offset, int size) {
        return notificationRepository.selectAllNotifications(userId, offset, size);
    }

    @Override
    public void markAsRead(Long notiNo) {
        notificationRepository.updateReadStatus(notiNo);
    }

    @Override
    public void createNotification(NotificationVO notification) {
        notificationRepository.insertNotification(notification);

    }

    // NotificationServiceImpl.java 에 추가
    @Override
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
