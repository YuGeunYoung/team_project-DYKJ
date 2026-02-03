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

    // [수정] 페이징 처리를 위해 offset과 size를 파라미터로 받습니다.
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

    // [추가] 모두 읽음 처리 API
    @PutMapping("/read-all/{userId}")
    public void markAllAsRead(@PathVariable String userId) {
        notificationService.markAllAsRead(userId);
    }
}