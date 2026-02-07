package com.example.studycafe.service;

import com.example.studycafe.dto.SeatResponseDto;
import com.example.studycafe.model.ActivityType;
import com.example.studycafe.model.History;
import com.example.studycafe.model.Member;

import com.example.studycafe.model.Seat;
import com.example.studycafe.repository.HistoryRepository;
import com.example.studycafe.repository.MemberRepository;
import com.example.studycafe.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;
    private final HistoryRepository historyRepository;
    private final MemberRepository memberRepository;

    public void reserve(Long seatId, Member member, long durationMinutes){
        // System Log 좌석 예약 요청
        log.info("좌석 예약 요청 - seatId: {}, user: {}, duration: {}시간", seatId, member.getEmail(), durationMinutes);

        // 1인 1좌석 제한
        Optional<Seat> existingSeat = seatRepository.findByMemberEmail(member.getEmail());
        if (existingSeat.isPresent()) {
            throw new IllegalStateException("already_registered"); // 중복 예약 예외 발생
        }
        // 시간 잔액 체크
        long currentMinutes = member.getRemainingMinutes() == null ? 0 : member.getRemainingMinutes();

        if (currentMinutes < durationMinutes) {
            throw new IllegalStateException("잔여 시간이 부족합니다. 시간을 충전해주세요.");
        }
        // 예약좌석 조회
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new IllegalArgumentException("해당좌석이 존재하지 않습니다."));
        // 좌석이 이미 사용 중인지 확인
        if(seat.isFull()){
            // System Log 예외 상황 경고 로그
            log.warn("예약 실패: 이미 점유된 좌석 - seatId: {}", seatId);
            throw new IllegalStateException("해당좌석은 이미 예약되었습니다.");
        }
        // 예약 처리 (시간 차감)
        member.setRemainingMinutes(currentMinutes - durationMinutes);
        memberRepository.save(member); // 변경된 시간 저장

        seat.setFull(true);
        seat.setMember(member);
        seat.setEndTime(LocalDateTime.now().plusMinutes(durationMinutes));

        // 예약 히스토리 저장
        saveHistory(member, ActivityType.RESERVE, seat.getSeatNumber() + "번 좌석 예약 (" + durationMinutes + "분 사용)");
        // System Log 좌석 예약 완료
        log.info("좌석 예약 완료 - 남은 시간: {}분", member.getRemainingMinutes());
    }
    // 자동 퇴실 스케줄러 (1분마다 실행)
    @Scheduled(fixedRate = 60000)
    public void autoCheckOut() {
        LocalDateTime now = LocalDateTime.now();

        // 종료 시간이 현재 시간보다 과거인(이미 지난) 좌석들을 모두 찾음
        List<Seat> expiredSeats = seatRepository.findByEndTimeBeforeAndIsFullTrue(now);

        for (Seat seat : expiredSeats) {
            Member member = seat.getMember();
            log.info("이용 시간 종료로 자동 퇴실 - seatNumber: {}, user: {}", seat.getSeatNumber(), member.getEmail());

            // 히스토리 기록
            saveHistory(member, ActivityType.FORCE_CANCEL, "이용 시간 종료 자동 퇴실");

            // 퇴실 처리
            seat.setMember(null);
            seat.setFull(false);
            seat.setEndTime(null);
        }
    }
    public void forceCancel(Long seatId) {
        //System Log 관리자 강제 취소 요청
        log.info("관리자 강제 취소 요청 - seatId: {}", seatId);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        Member member = seat.getMember(); // 취소하기 전에 멤버 정보 확보
        if (member != null) {
            // [추가된 로직] 남은 시간이 있다면 지갑으로 복구(환불)
            if (seat.getEndTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (seat.getEndTime().isAfter(now)) {
                    Duration duration = Duration.between(now, seat.getEndTime());
                    long refundMinutes = duration.toMinutes();

                    if (refundMinutes > 0) {
                        long currentMinutes = member.getRemainingMinutes() == null ? 0 : member.getRemainingMinutes();
                        member.setRemainingMinutes(currentMinutes + refundMinutes);
                        // SyStem Log 강제 퇴실 처리
                        log.info("강제 퇴실 환불 처리 - 사용자: {}, 환불 시간: {}분", member.getEmail(), refundMinutes);
                    }
                }
            }
            // 히스토리 저장
            saveHistory(member, ActivityType.FORCE_CANCEL, "관리자에 의한 강제 퇴실 (잔여 시간 반환됨)");
        }

        // 예약자 정보 삭제
        seat.setFull(false);
        seat.setMember(null);
        seat.setEndTime(null);
        // System Log 강제 취소 완료
        log.info("강제 취소 완료 - seatId: {}", seatId);
    }

    public void cancel(String email){
        // System Log 예약 취소 요청
        log.info("예약 취소 요청 - user: {}", email);

        Seat seat = seatRepository.findByMemberEmail(email)
                .orElseThrow(() -> new IllegalStateException("예약한 좌석이 없습니다."));

        Member member = seat.getMember(); // 취소하기 전에 멤버 정보 확보
        // 남은 시간 계산 및 환불
        if (seat.getEndTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (seat.getEndTime().isAfter(now)) {
                // 종료시간 - 현재시간 = 남은 시간
                Duration duration = Duration.between(now, seat.getEndTime());
                long refundMinutes = duration.toMinutes();
                if (refundMinutes > 0) {
                    long currentMinutes = member.getRemainingMinutes() == null ? 0 : member.getRemainingMinutes();
                    member.setRemainingMinutes(currentMinutes + refundMinutes);
                    log.info("조기 퇴실 환불 - 환불된 시간: {}분", refundMinutes);
                }
            }
        }
        // 취소 히스토리 저장
        saveHistory(member, ActivityType.CANCEL, seat.getSeatNumber() + "번 좌석 이용 종료");

        // 좌석정보 초기화
        seat.setFull(false);
        seat.setMember(null);
        seat.setEndTime(null);
        // System Log 예약 취소 완료
        log.info("예약 취소 완료 - seatNumber: {}", seat.getSeatNumber());
    }

    // 시간 충전
    public void chargeTime(String email, int hours) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        long chargeMinutes = hours * 60L;

        // 일단 충전
        long currentWallet = member.getRemainingMinutes() == null ? 0 : member.getRemainingMinutes();
        member.setRemainingMinutes(currentWallet + chargeMinutes);

        // 만약 현재 이용 중인 좌석이 있다면 자동 연장
        Optional<Seat> activeSeat = seatRepository.findByMemberEmail(email);
        if (activeSeat.isPresent()) {
            Seat seat = activeSeat.get();
            // 차감
            member.setRemainingMinutes(member.getRemainingMinutes() - chargeMinutes);
            // 좌석 시간 연장
            if(seat.getEndTime() == null || seat.getEndTime().isBefore(LocalDateTime.now())) {
                seat.setEndTime(LocalDateTime.now().plusMinutes(chargeMinutes));
            } else {
                seat.setEndTime(seat.getEndTime().plusMinutes(chargeMinutes));
            }
            log.info("이용 중인 좌석({}) 시간 자동 연장 완료.", seat.getSeatNumber());
        }

        memberRepository.save(member);
        saveHistory(member, ActivityType.RESERVE, "시간 충전: " + hours + "시간" + (activeSeat.isPresent() ? " (자동 연장)" : ""));
    }

    public List<SeatResponseDto> findAllSeats() {
        return seatRepository.findAll().stream().map(SeatResponseDto::new).collect(Collectors.toList());
    }

    private void saveHistory(Member member, ActivityType type, String description) {
        History history = History.builder()
                .member(member)
                .type(type)
                .description(description)
                .build();
        historyRepository.save(history);
    }
}
