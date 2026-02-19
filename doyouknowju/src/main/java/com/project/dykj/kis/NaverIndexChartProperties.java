package com.project.dykj.kis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "naver.index-chart")
public class NaverIndexChartProperties {
	private String kospiUrl;
	private String kosdaqUrl;
	private Duration timeout = Duration.ofSeconds(5);
	private Duration cacheTtl = Duration.ofSeconds(20);
}
