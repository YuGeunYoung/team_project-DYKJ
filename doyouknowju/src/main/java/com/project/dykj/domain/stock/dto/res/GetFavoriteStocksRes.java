package com.project.dykj.domain.stock.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetFavoriteStocksRes {
    private String stockId;
    private String stockName;
}
