package com.project.dykj.domain.notification.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.project.dykj.domain.notification.dto.NotificationVO;
import com.project.dykj.domain.notification.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // [설명] 특정 유저(userId)의 알림 목록을 조회하는 API입니다.
    // 프론트엔드에서 /dykj/api/notifications/{userId} 로 요청하면 호출됩니다.
    @GetMapping("/{userId}")
    public List<NotificationVO> getNotificationList(@PathVariable String userId) {
        return notificationService.getNotificationList(userId);
    }

    // [설명] 특정 알림(notiNo)을 '읽음' 처리 하는 API입니다.
    // 알림을 클릭했을 때 호출되어 IS_READ 상태를 'Y'로 변경합니다.
    @PutMapping("/read/{notiNo}")
    public void markAsRead(@PathVariable Long notiNo) {
        notificationService.markAsRead(notiNo);
    }
}
