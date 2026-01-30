package com.project.dykj.common.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.project.dykj.domain.member.entity.Member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class LoginInterceptor implements HandlerInterceptor{
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception{
		if(request.getMethod().equals("OPTIONS")) {
			return true;
		}
		
		HttpSession session = request.getSession(false);
		
		if(session != null) {
			Member loginUser = (Member) session.getAttribute("loginUser");
			if(loginUser != null) {
				return true;
			}
		}
		
		response.setContentType("application/json; charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
		return false;
	}
}