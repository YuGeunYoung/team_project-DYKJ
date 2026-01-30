package com.project.dykj.domain.stock.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Holding {
    private String userId;
    private String stockId;
    private long totalPrice;
    private long totalCount;
}
