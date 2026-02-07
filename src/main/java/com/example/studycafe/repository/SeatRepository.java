package com.example.studycafe.repository;

import com.example.studycafe.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    Optional<Seat> findByMemberEmail(String email);
    List<Seat> findByEndTimeBeforeAndIsFullTrue(LocalDateTime time);
}
