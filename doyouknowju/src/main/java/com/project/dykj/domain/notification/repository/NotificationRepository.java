package com.project.dykj.domain.notification.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.project.dykj.domain.notification.dto.NotificationVO;

@Mapper
public interface NotificationRepository {
    // [설명] 특정 유저의 알림을 최신순으로 조회 (SQL ID: selectAllNotifications)
    List<NotificationVO> selectAllNotifications(String userId);

    // [설명] 새로운 알림을 DB에 저장 (SQL ID: insertNotification)
    int insertNotification(NotificationVO notification);

    // [설명] 알림을 읽음(Y) 상태로 변경 (SQL ID: updateReadStatus)
    int updateReadStatus(Long notiNo);
    
    /**
     * [추가] 오늘 특정 유저에게 특정 종목의 알림을 보냈는지 개수를 셉니다.
     */
    int countTodayStockNotification(@Param("userId") String userId, @Param("stockId") String stockId);
    
}
