package com.project.dykj.domain.board.model.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Reply {
    private int replyId;
    private String replyContent;
    private LocalDateTime createDate;
    private int boardId;
    private String userId;
    private LocalDateTime deleteDate;
    private LocalDateTime modifyDate;
}
