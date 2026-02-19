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
public class Board {
    private int boardId;
    private String boardTitle;
    private String boardContent;
    private LocalDateTime createDate;
    private LocalDateTime modifyDate;
    private LocalDateTime deleteDate;
    private int viewCount;
    private String stockId;
    private String stockName;
    private String boardType;
    private String userId;
    private String userTitle;
    private String userTitleImgUrl;
    private String userColor;
}
