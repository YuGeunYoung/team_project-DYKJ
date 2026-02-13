// ChatMessageVO.java
package com.project.dykj.domain.chat.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ChatMessageVO {
    private Long chatId;
    private String userId;
    private String chatContent;
    private Date sendTime;

    private String userTitle; // 칭호 이름
    private String userTitleColor; // 칭호 색상
    private String userTitleImgUrl; //[taek] ChatHanler 오류 수정
}