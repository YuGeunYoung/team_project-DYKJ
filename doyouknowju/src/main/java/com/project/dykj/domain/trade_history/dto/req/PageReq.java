package com.project.dykj.domain.trade_history.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PageReq {
    private int page;
    private int groupSize;
    private int start;
    private int end;
}
