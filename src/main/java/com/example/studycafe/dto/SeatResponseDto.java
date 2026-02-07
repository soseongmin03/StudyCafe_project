package com.example.studycafe.dto;

import com.example.studycafe.model.Seat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SeatResponseDto { // dto 사용시 필요없는 정보까지 넘어오는 것을 방지할 수 있음
    private Long id;
    private int seatNumber;
    private boolean isFull;
    private String reservedByName; // 예약자 이름 (개인정보 보호를 위해 전체 객체 대신 이름만 노출)
    private String endTimeStr; // 시간
    private String remainingTimeStr; // 남은시간
    // Entity -> DTO 변환 생성자
    public SeatResponseDto(Seat seat) {
        this.id = seat.getId();
        this.seatNumber = seat.getSeatNumber();
        this.isFull = seat.isFull();

        // 남은 시간 계산
        if (seat.getEndTime() != null && seat.isFull()) {
            // 현재 시간과 종료 시간 사이의 차이 구하기
            Duration duration = Duration.between(LocalDateTime.now(), seat.getEndTime());

            // 시간이 다 됐거나 지났으면 "00:00" 표시
            if (duration.isNegative() || duration.isZero()) {
                this.remainingTimeStr = "00:00";
            } else {
                // 시간과 분 추출
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                // "00:00" 형식으로 포맷팅 (%02d는 두 자리 숫자로 채운다는 뜻)
                this.remainingTimeStr = String.format("%02d:%02d", hours, minutes);
            }
        } else {
            this.remainingTimeStr = "";
        }

        // 예약자가 있을 때만 이름을 넣고, 없으면 null 혹은 비어있음 처리
        if (seat.getMember() != null) {
            this.reservedByName = seat.getMember().getName(); // Member 엔티티의 일부 정보만 선택적 노출
        } else {
            this.reservedByName = null;
        }
    }
}