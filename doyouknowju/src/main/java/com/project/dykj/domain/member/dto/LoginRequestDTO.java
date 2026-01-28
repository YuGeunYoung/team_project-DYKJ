package com.project.dykj.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
	private String userId;
	private String userPwd;
}
