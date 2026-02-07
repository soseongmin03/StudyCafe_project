package com.example.studycafe.controller;

import com.example.studycafe.model.History;
import com.example.studycafe.model.Member;
import com.example.studycafe.repository.HistoryRepository;
import com.example.studycafe.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MemberService memberService;
    private final HistoryRepository historyRepository;

    @GetMapping("/mypage")
    public String myPage(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login"; // 로그인 안 했으면 쫓아내기
        }

        // 1. 현재 로그인한 사용자의 이메일 가져오기
        String email = authentication.getName();

        // 2. 이메일로 멤버 정보 찾기
        Member member = memberService.findOne(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));

        // 3. 내 ID로 히스토리 찾기 (최신순 정렬)
        List<History> myHistories = historyRepository.findByMemberId(
                member.getId(),
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        // 4. 뷰에 전달
        model.addAttribute("member", member);
        model.addAttribute("histories", myHistories);

        return "mypage";
    }
}