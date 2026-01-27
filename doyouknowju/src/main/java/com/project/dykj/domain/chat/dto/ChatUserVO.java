// ChatUserVO.java
package com.project.dykj.domain.chat.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ChatUserVO {
    private String userId;
    private String isOnline;
    private Date lastActive;
}