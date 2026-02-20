package com.project.dykj.kis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "naver.index-chart")
public class NaverIndexChartProperties {
	private String apiBaseUrl = "https://api.stock.naver.com/chart/domestic/index";
	private String defaultRange = "day";
	private String kospiUrl;
	private String kosdaqUrl;
	private Duration timeout = Duration.ofSeconds(5);
	private Duration cacheTtl = Duration.ofSeconds(20);
}
