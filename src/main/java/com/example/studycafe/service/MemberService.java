package com.example.studycafe.service;

import com.example.studycafe.dto.MemberFormDto;
import com.example.studycafe.model.ActivityType;
import com.example.studycafe.model.History;
import com.example.studycafe.model.Member;
import com.example.studycafe.repository.HistoryRepository;
import com.example.studycafe.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional // 에러 발생 시 모든 변경사항을 이전으로 되돌립니다
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성하여 의존성을 주입합니다.
public class MemberService {
    private final MemberRepository memberRepository;
    private final HistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;
    //회원 가입
    public Long join(MemberFormDto formDto) {
        // System Log 회원가입 요청
        log.info("회원가입 요청 - email: {}, name: {}", formDto.getEmail(), formDto.getName());

        // 1. 중복 회원 검증 (이메일 String으로 검증)
        validateDuplicateMember(formDto.getEmail());

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(formDto.getPassword());

        // 3. DTO -> Entity 변환 (권한 설정 포함)
        Member member = formDto.toEntity(encodedPassword);

        // 4. 저장
        memberRepository.save(member);
        //히스토리 저장
        History history = History.builder()
                .member(member)
                .type(ActivityType.SIGNUP)
                .description("신규 회원 가입 완료")
                .build();
        historyRepository.save(history);

        // System Log 처리 완료 로그
        log.info("회원가입 성공 - memberId: {}", member.getId());

        return member.getId();
    }
    // 중복 이메일 체크 (파라미터를 Member 객체에서 String email로 변경하여 재사용성 높임)
    private void validateDuplicateMember(String email) {
        memberRepository.findByEmail(email)
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 가입된 이메일입니다.");
                });
    }

    // 회원 정보 단건 조회
    @Transactional(readOnly = true)
    public Optional<Member> findOne(String email) {
        return memberRepository.findByEmail(email);
    }
}