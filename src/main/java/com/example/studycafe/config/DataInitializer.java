package com.example.studycafe.config;

import com.example.studycafe.model.Member;
import com.example.studycafe.model.Seat;
import com.example.studycafe.repository.MemberRepository;
import com.example.studycafe.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeatRepository seatRepository;
    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (memberRepository.findByEmail("sosungmin03@naver.com").isEmpty()) {
            memberRepository.save(Member.builder()
                    .name("소성민")
                    .email("sosungmin03@naver.com")
                    .age(24)
                    .password(passwordEncoder.encode("030122"))
                    .role("ROLE_ADMIN").build());
            System.out.println("--- 관리자 계정이 생성되었습니다");
        }
        if (seatRepository.count() == 0) { // 좌석이 하나도 없을 때만 실행
            for (int i = 1; i <= 30; i++) {
                seatRepository.save(Seat.builder()
                        .seatNumber(i)
                        .isFull(false)
                        .build());
            }
            System.out.println("--- 30개의 좌석이 생성되었습니다 ---");
        }
    }
}
