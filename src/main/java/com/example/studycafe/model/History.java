package com.example.studycafe.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 누가 했는지 기록 (회원 탈퇴 시 로그도 같이 지울지, 남길지 결정해야 함. 여기선 남기기 위해 연관관계 느슨하게 설계 가능하지만, 일단 FM대로 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING) // Enum 이름을 DB에 문자열("RESERVE")로 저장
    @Column(length = 50)
    private ActivityType type;

    private String description; // 상세 내용 (예: "1번 좌석 예약")

    @CreationTimestamp // INSERT 시 자동으로 현재 시간 기록
    private LocalDateTime createdDate;
}