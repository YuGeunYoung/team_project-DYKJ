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

    // [설명] DB(Repository)에서 해당 유저의 모든 알림을 조회하여 반환합니다.
    @Override
    public List<NotificationVO> getNotificationList(String userId) {
        return notificationRepository.selectAllNotifications(userId);
    }

    // [설명] DB(Repository)에 요청하여 해당 알림 번호의 읽음 상태(IS_READ)를 업데이트합니다.
    @Override
    public void markAsRead(Long notiNo) {
        notificationRepository.updateReadStatus(notiNo);
    }

    // [설명] DB(Repository)에 요청하여 새로운 알림 데이터를 삽입(Insert)합니다.
    @Override
    public void createNotification(NotificationVO notification) {
        notificationRepository.insertNotification(notification);
    }
}
