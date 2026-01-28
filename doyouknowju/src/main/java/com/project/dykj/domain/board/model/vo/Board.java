package com.project.dykj.domain.board.model.vo;

import java.sql.Date;

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
    private Date createDate;
    private Date modifyDate;
    private Date deleteDate;
    private int viewCount;
    private String stockId;
    private String boardType;
    private String userId;
}

