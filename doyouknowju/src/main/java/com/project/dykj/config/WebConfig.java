package com.project.dykj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.project.dykj.common.interceptor.LoginInterceptor;

import lombok.RequiredArgsConstructor;

//로그인 확인 및 정보 전달용 인터셉터 적용 설정
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer{

	private final LoginInterceptor loginInterceptor;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loginInterceptor)
				.addPathPatterns("/api/members/info",
								 "/api/members/logout",
								 "/api/game/**"
				);
	}
}
