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

    // [수정] offset과 size를 @RequestParam으로 받습니다. 값이 없으면 기본값(0, 20)을 사용합니다.
    @GetMapping("/{userId}")
    public List<NotificationVO> getNotificationList(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.getNotificationList(userId, offset, size);
    }

    @PutMapping("/read/{notiNo}")
    public void markAsRead(@PathVariable Long notiNo) {
        notificationService.markAsRead(notiNo);
    }
}