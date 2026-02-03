package com.project.dykj.domain.notification.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.project.dykj.domain.notification.dto.NotificationVO;

@Mapper
public interface NotificationRepository {
    List<NotificationVO> selectAllNotifications(@Param("userId") String userId, @Param("offset") int offset, @Param("size") int size);
    int insertNotification(NotificationVO notification);
    int updateReadStatus(Long notiNo);
    int countTodayStockNotification(@Param("userId") String userId, @Param("stockId") String stockId);

    // [추가] 모두 읽음 처리
    int markAllAsRead(String userId);
}