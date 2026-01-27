package com.project.dykj.domain.member.dto;

import lombok.Data;

@Data
public class MemberVO {
	private String userId;
	private String userPwd;
	private String phone;
	private String isReceiveNotification;
}
