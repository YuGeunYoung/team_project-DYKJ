package com.project.dykj.domain.notification.repository;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.project.dykj.domain.notification.dto.NotificationVO;

@Mapper
public interface NotificationRepository {
    // [설명] 특정 유저의 알림을 최신순으로 조회 (SQL ID: selectAllNotifications)
    List<NotificationVO> selectAllNotifications(String userId);

    // [설명] 새로운 알림을 DB에 저장 (SQL ID: insertNotification)
    int insertNotification(NotificationVO notification);

    // [설명] 알림을 읽음(Y) 상태로 변경 (SQL ID: updateReadStatus)
    int updateReadStatus(Long notiNo);
}
