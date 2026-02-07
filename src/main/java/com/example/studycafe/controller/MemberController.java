package com.example.studycafe.controller;
import com.example.studycafe.dto.MemberFormDto;
import com.example.studycafe.dto.SeatResponseDto;
import com.example.studycafe.model.Member;
import com.example.studycafe.model.Seat;
import com.example.studycafe.repository.SeatRepository;
import com.example.studycafe.service.MemberService;
import com.example.studycafe.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final SeatService seatService;
    private final SeatRepository seatRepository;
    @GetMapping({"/","/home"})
    public String gethome(){return "home";}

    @GetMapping("/book")
    public String getBook(Model model, Authentication authentication) {
        List<SeatResponseDto> seats = seatService.findAllSeats();
        model.addAttribute("seats", seats);
        long totalDisplayMinutes = 0;

        // 로그인한 경우 잔여 시간을 모델에 담아서 보냄
        if (authentication != null) {
            String email = authentication.getName();
            Member member = memberService.findOne(email).orElse(null);

            if (member != null) {
                // 현재 남은 시간
                long walletTime = member.getRemainingMinutes() == null ? 0 : member.getRemainingMinutes();

                // 좌석에 걸려있는 시간 (이용 중이라면)
                long seatTime = 0;
                Optional<Seat> mySeat = seatRepository.findByMemberEmail(email);
                if (mySeat.isPresent() && mySeat.get().getEndTime() != null) {
                    Duration duration = Duration.between(LocalDateTime.now(), mySeat.get().getEndTime());
                    if (!duration.isNegative()) {
                        seatTime = duration.toMinutes();
                    }
                }

                // 사용자에게 보여줄 시간
                totalDisplayMinutes = walletTime + seatTime;
            }
        }

        model.addAttribute("remainingMinutes", totalDisplayMinutes);

        return "book";
    }

    @PostMapping("/book/{seatId}") //seat.Id 변수명 적어주가
    public String reverseSeat(@PathVariable("seatId") Long seatId,
                              @RequestParam(value = "duration", required = false, defaultValue = "0") int durationHours,
                              @RequestParam(value = "useAll", required = false, defaultValue = "false") boolean useAll,
                              Authentication authentication){
        String email = authentication.getName();
        Member member = memberService.findOne(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        try {
            long durationMinutes;

            // useAll=true이면 보유한 모든 시간을 사용
            if (useAll) {
                durationMinutes = member.getRemainingMinutes();
            } else {
                // 결제 후 예약인 경우: 먼저 충전하고 예약 진행
                seatService.chargeTime(email, durationHours);
                // 방금 충전했으므로 durationHours * 60 만큼 예약
                durationMinutes = durationHours * 60L;
            }
            seatService.reserve(seatId, member, durationMinutes);

        } catch (IllegalStateException e) {
            if ("already_registered".equals(e.getMessage())) {
                return "redirect:/book?alreadyRegistered=true";
            }
            return "redirect:/book?error=" + e.getMessage();
        }
        return "redirect:/book";
    }
    // 시간 충전 (좌석 예약 없이 시간만 충전)
    @PostMapping("/book/charge")
    public String chargeTime(@RequestParam("duration") int duration, Authentication authentication) {
        seatService.chargeTime(authentication.getName(), duration);
        return "redirect:/book";
    }

    @PostMapping("/book/cancel")
    public String cancelReverse(Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        try {
            seatService.cancel(authentication.getName());
        } catch (IllegalStateException e) {
            // "예약한 좌석이 없습니다" 메시지를 전달하기 위해 파라미터 사용
            return "redirect:/book?noBooking=true";
        }

        return "redirect:/book";
    }

    @GetMapping("/signup")
    public String getsignup(Model model){
        // 뷰(Thymeleaf)에서 사용할 빈 DTO 객체를 전달합니다.
        model.addAttribute("memberFormDto", new MemberFormDto());
        return "signup";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/signup")
    public String postsignup(@Valid MemberFormDto memberFormDto,
                             BindingResult bindingResult,
                             Model model) {
        // 검증 에러가 있으면 다시 가입 페이지로 보냄
        if (bindingResult.hasErrors()) {
            return "signup";
        }
        try {
            // 정상 로직 수행
            memberService.join(memberFormDto);
        } catch (IllegalStateException e) {
            // 중복 회원 예외 발생 시 에러 메시지를 담아서 다시 보냄
            model.addAttribute("errorMessage", e.getMessage());
            return "signup";
        }
        return "redirect:/login";
    }


}
