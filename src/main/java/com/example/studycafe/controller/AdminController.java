package com.example.studycafe.controller;

import com.example.studycafe.model.History;
import com.example.studycafe.model.Seat;
import com.example.studycafe.repository.HistoryRepository;
import com.example.studycafe.repository.SeatRepository;
import com.example.studycafe.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Controller
@RequiredArgsConstructor
public class AdminController {

    private final SeatRepository seatRepository;
    private final SeatService seatService;
    private final HistoryRepository historyRepository;

    // 전체 좌석 현황 관리 페이지
    @GetMapping("/member/admin")
    public String adminDashboard(Model model) {
        List<Seat> seats = seatRepository.findAll(); // 모든 좌석 리스트 조회
        long occupiedCount = seats.stream().filter(Seat::isFull).count();

        model.addAttribute("seats", seats);
        model.addAttribute("occupiedCount", occupiedCount);
        model.addAttribute("totalCount", seats.size());

        return "admin/dashboard";
    }

    @GetMapping("/admin/logs")
    public String viewLogs(Model model) {
        // 모든 기록을 가져오되, 'createdDate' 기준 내림차순(DESC)으로 정렬 (최신이 위로)
        List<History> logs = historyRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));

        model.addAttribute("logs", logs);
        return "admin/logs";
    }

    // 관리자 권한으로 좌석 강제 취소
    @PostMapping("/member/admin/cancel/{seatId}")
    public String adminCancel(@PathVariable("seatId") Long seatId) {
        seatService.forceCancel(seatId); // 강제 취소 로직 호출
        return "redirect:/member/admin";
    }
}
