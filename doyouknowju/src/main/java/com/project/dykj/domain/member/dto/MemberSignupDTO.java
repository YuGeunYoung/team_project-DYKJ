package com.project.dykj.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberSignupDTO {
	private String userId;
	private String userPwd;
	private String phone;
	private String isRecieveNotification;
}
