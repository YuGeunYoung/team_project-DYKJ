package com.project.dykj.domain.member.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MemberRequestDTO {
	private String userId;
	private String userPwd;
	private String phone;
	private String isReceiveNotification;
}
