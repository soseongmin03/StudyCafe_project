package com.example.studycafe.service;

import com.example.studycafe.model.History;
import com.example.studycafe.model.Member;
import com.example.studycafe.model.Seat;
import com.example.studycafe.repository.HistoryRepository;
import com.example.studycafe.repository.MemberRepository;
import com.example.studycafe.repository.SeatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private HistoryRepository historyRepository;

    @InjectMocks
    private SeatService seatService;

    // ==========================================
    // 1. 예약(Reserve) 테스트
    // ==========================================

    @Test
    @DisplayName("좌석 예약 성공: 잔여 시간이 충분할 때 정상적으로 차감되고 예약된다.")
    void reserve_success() {
        // given
        Long seatId = 1L;
        long durationMinutes = 60L; // 60분 예약
        Member member = Member.builder()
                .id(1L)
                .email("test@test.com")
                .remainingMinutes(100L) // 100분 보유
                .build();
        Seat seat = Seat.builder().id(seatId).seatNumber(1).isFull(false).build();

        // mocking
        given(seatRepository.findByMemberEmail(member.getEmail())).willReturn(Optional.empty()); // 중복 예약 없음
        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat)); // 좌석 존재함

        // when
        seatService.reserve(seatId, member, durationMinutes);

        // then
        assertThat(seat.isFull()).isTrue(); // 좌석 점유 확인
        assertThat(seat.getMember()).isEqualTo(member); // 좌석 주인 확인
        assertThat(member.getRemainingMinutes()).isEqualTo(40L); // 100 - 60 = 40분 남음 확인
        assertThat(seat.getEndTime()).isAfter(LocalDateTime.now()); // 종료 시간이 현재 이후인지 확인

        // 히스토리 저장 호출 여부 확인
        verify(historyRepository, times(1)).save(any(History.class));
    }

    @Test
    @DisplayName("좌석 예약 실패: 잔여 시간이 부족하면 예외가 발생한다.")
    void reserve_fail_not_enough_time() {
        // given
        Long seatId = 1L;
        long durationMinutes = 60L;
        Member member = Member.builder()
                .email("test@test.com")
                .remainingMinutes(30L) // 30분밖에 없음 (부족!)
                .build();

        given(seatRepository.findByMemberEmail(member.getEmail())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> seatService.reserve(seatId, member, durationMinutes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔여 시간이 부족합니다");
    }

    @Test
    @DisplayName("좌석 예약 실패: 이미 다른 사람이 사용 중인 좌석")
    void reserve_fail_seat_occupied() {
        // given
        Long seatId = 1L;
        long durationMinutes = 60L;
        Member member = Member.builder().email("test@test.com").remainingMinutes(100L).build();
        Seat seat = Seat.builder().id(seatId).isFull(true).build(); // 이미 찬 좌석

        given(seatRepository.findByMemberEmail(member.getEmail())).willReturn(Optional.empty());
        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        // when & then
        assertThatThrownBy(() -> seatService.reserve(seatId, member, durationMinutes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 예약되었습니다");
    }

    // ==========================================
    // 2. 시간 충전(Charge) 테스트
    // ==========================================

    @Test
    @DisplayName("시간 충전: 이용 중인 좌석이 없을 때는 지갑 시간만 늘어난다.")
    void charge_success_no_seat() {
        // given
        String email = "test@test.com";
        int chargeHours = 2; // 2시간(120분) 충전
        Member member = Member.builder().email(email).remainingMinutes(10L).build(); // 기존 10분

        given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
        given(seatRepository.findByMemberEmail(email)).willReturn(Optional.empty()); // 이용 중인 좌석 없음

        // when
        seatService.chargeTime(email, chargeHours);

        // then
        assertThat(member.getRemainingMinutes()).isEqualTo(130L); // 10 + 120 = 130분
    }

    @Test
    @DisplayName("시간 충전: 이용 중인 좌석이 있을 때는 좌석 시간도 자동 연장된다.")
    void charge_success_with_active_seat() {
        // given
        String email = "test@test.com";
        int chargeHours = 1; // 60분 충전
        Member member = Member.builder().email(email).remainingMinutes(0L).build();

        // 현재 10분 뒤에 끝나는 좌석 이용 중
        LocalDateTime originalEndTime = LocalDateTime.now().plusMinutes(10);
        Seat seat = Seat.builder().endTime(originalEndTime).member(member).seatNumber(1).build();

        given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
        given(seatRepository.findByMemberEmail(email)).willReturn(Optional.of(seat));

        // when
        seatService.chargeTime(email, chargeHours);

        // then
        // 1. 지갑: 충전되자마자 좌석 연장에 사용되었으므로 0분 유지
        assertThat(member.getRemainingMinutes()).isEqualTo(0L);

        // 2. 좌석: 종료 시간이 기존보다 60분 늘어났어야 함 (오차 범위 감안하여 after 체크)
        assertThat(seat.getEndTime()).isAfter(originalEndTime);
    }

    // ==========================================
    // 3. 퇴실 및 환불(Cancel) 테스트
    // ==========================================

    @Test
    @DisplayName("퇴실: 남은 시간이 있으면 환불되고 좌석이 비워진다.")
    void cancel_success_with_refund() {
        // given
        String email = "test@test.com";
        Member member = Member.builder().email(email).remainingMinutes(0L).build();

        // 30분 남은 좌석
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(30);
        Seat seat = Seat.builder().member(member).isFull(true).endTime(endTime).seatNumber(1).build();

        given(seatRepository.findByMemberEmail(email)).willReturn(Optional.of(seat));

        // when
        seatService.cancel(email);

        // then
        assertThat(seat.isFull()).isFalse(); // 좌석 비워짐
        assertThat(seat.getMember()).isNull(); // 주인 없어짐
        assertThat(seat.getEndTime()).isNull(); // 시간 초기화

        // 환불 확인 (30분 정도가 환불되어야 함 - 실행 시간 차이로 29~30분)
        assertThat(member.getRemainingMinutes()).isGreaterThanOrEqualTo(29L);
    }

    // ==========================================
    // 4. 관리자 강제 퇴실(Force Cancel) 테스트
    // ==========================================

    @Test
    @DisplayName("관리자 강제 퇴실: 사용자에게 남은 시간을 환불해주고 쫓아낸다.")
    void force_cancel_success() {
        // given
        Long seatId = 1L;
        Member member = Member.builder().email("target@user.com").remainingMinutes(10L).build();

        // 50분 남은 좌석
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(50);
        Seat seat = Seat.builder().id(seatId).member(member).isFull(true).endTime(endTime).seatNumber(1).build();

        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        // when
        seatService.forceCancel(seatId);

        // then
        assertThat(seat.isFull()).isFalse();
        assertThat(seat.getMember()).isNull();

        // 원래 10분 + 환불 50분 = 약 60분
        assertThat(member.getRemainingMinutes()).isGreaterThanOrEqualTo(59L);

        verify(historyRepository, times(1)).save(any(History.class));
    }
}