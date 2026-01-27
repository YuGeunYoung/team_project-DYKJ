package com.project.dykj.domain.member.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.member.dto.MemberSignupDTO;
import com.project.dykj.domain.member.dto.MemberVO;
import com.project.dykj.domain.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/members")
@Slf4j
@RequiredArgsConstructor
public class MemberController {
	
	private final MemberService service;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MemberSignupDTO dto) {
        log.info("회원가입 요청: {}", dto.getUserId());

        try {
            // 1. 아이디 중복 확인 (서비스를 통해 DAO 호출)
            int duplicateCount = service.checkId(dto.getUserId());
            if (duplicateCount > 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 아이디입니다."));
            }

            // 2. 비밀번호 암호화
            String securePwd = passwordEncoder.encode(dto.getUserPwd());

            // 3. VO 객체에 데이터 담기
            MemberVO vo = new MemberVO();
            vo.setUserId(dto.getUserId());
            vo.setUserPwd(securePwd); // 암호화된 비번 세팅
            vo.setPhone(dto.getPhone());
            vo.setIsReceiveNotification(dto.isReceiveNotification() ? "Y" : "N");

            // 4. 서비스로 VO 전달 (최종 저장 요청)
            service.signup(vo);

            return ResponseEntity.ok().body(Map.of("message", "회원가입 성공"));

        } catch (Exception e) {
            log.error("회원가입 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "서버 오류 발생"));
        }
    }
}
