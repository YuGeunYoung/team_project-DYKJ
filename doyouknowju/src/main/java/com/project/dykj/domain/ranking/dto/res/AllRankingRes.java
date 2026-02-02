package com.project.dykj.domain.ranking.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AllRankingRes {
    private String userId;
    private long points;
    private long rank;
}
