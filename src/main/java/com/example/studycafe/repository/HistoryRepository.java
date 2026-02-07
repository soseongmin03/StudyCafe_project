package com.example.studycafe.repository;

import com.example.studycafe.model.History;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {
    // 특정 회원의 활동 내역만 찾아보는 기능 미리 추가
    List<History> findByMemberId(Long memberId);
    List<History> findByMemberId(Long memberId, Sort sort);
}