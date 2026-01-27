package com.project.dykj.domain.notification.repository;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.project.dykj.domain.notification.dto.NotificationVO;

@Mapper
public interface NotificationRepository { 
	
    // XML의 <select id="selectAllNotifications"> 와 연결됩니다.
    List<NotificationVO> selectAllNotifications();
	
}
