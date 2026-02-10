package com.project.dykj.domain.stock.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserIdAndStockIdReq {
    private String userId;
    private String stockId;
}
