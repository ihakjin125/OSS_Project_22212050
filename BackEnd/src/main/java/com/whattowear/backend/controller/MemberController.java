package com.whattowear.backend.controller;

import com.whattowear.backend.domain.Member;
import com.whattowear.backend.service.MemberService;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Getter @Setter
    public static class SignupRequest {
        private String loginId;
        private String password;
        private String nickname;
        private String location;

        private Double constitutionWeight;
        private Double constitution;
    }

    // 1. 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        Member member = new Member();
        member.setLoginId(request.getLoginId());
        member.setPassword(request.getPassword());
        member.setNickname(request.getNickname());
        member.setLocation(request.getLocation());

        if (request.getConstitutionWeight() != null) {
            member.setConstitutionWeight(request.getConstitutionWeight());
        } else if (request.getConstitution() != null) {
            member.setConstitutionWeight(request.getConstitution());
        } else {
            member.setConstitutionWeight(0.0);
        }

        memberService.join(member);
        return ResponseEntity.ok("회원가입이 완료되었습니다!");
    }

    // 2. 로그인 API (유지)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String loginId, @RequestParam String password) {
        Member member = memberService.login(loginId, password);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", member.getNickname() + "님 환영합니다!");
        responseData.put("nickname", member.getNickname());
        responseData.put("constitution", member.getConstitutionWeight());

        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/init-constitution")
    public ResponseEntity<String> initConstitution(@RequestParam String loginId, @RequestParam Double constitution) {
        Member member = memberService.findByLoginId(loginId);
        if (member == null) {
            return ResponseEntity.badRequest().body("회원을 찾을 수 없습니다.");
        }

        member.setBaseConstitutionWeight(constitution);
        member.setConstitutionWeight(constitution);
        memberService.save(member);

        return ResponseEntity.ok("체질 설정 완료!");
    }
}