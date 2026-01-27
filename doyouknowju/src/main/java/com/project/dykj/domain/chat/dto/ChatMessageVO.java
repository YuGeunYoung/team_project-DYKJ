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
}