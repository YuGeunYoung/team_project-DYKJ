package com.project.dykj.domain.member.entity;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
	private String userId;
	private String userPwd;
	private String phone;
	private long points; 
	private String status;
	private String userRole;
	private Date enrollDate;
	private int consecDays;
	private String isReceiveNotification;
	private Date banLimitDate;
	private int experience;
	private int userLevel;
}
