package com.project.dykj.domain.ranking.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RankingRes {
    private String userId;
    private long startPoint;
    private long currentPoint;
    private double returnRate;
}
