package com.example.vadoo.controller.btc;

import com.example.vadoo.dto.btc.FeedbackDTO;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.repository.SuKienRepository;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.btc.BTCDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/btc/feedback")
@RequiredArgsConstructor
public class BTCFeedbackController {

    private final BTCDashboardService btcService;
    private final SuKienRepository suKienRepository;

    @GetMapping
    public String viewFeedback(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam(required = false) Integer eventId,
                               Model model) {
        Integer btcId = user.getUser().getId();

        // 1. Common Data & Dropdown List
        model.addAttribute("user", user);
        model.addAttribute("stats", btcService.getDashboardStats(btcId));

        List<SuKien> events = suKienRepository.findByBtcIdOrderByIdDesc(btcId);
        model.addAttribute("events", events);
        model.addAttribute("currentEventId", eventId);

        // 2. Lấy Feedback List (QUAN TRỌNG: Lấy dữ liệu trước để tính toán)
        // Hàm này ở Service đã xử lý: nếu eventId null -> lấy hết, nếu có eventId -> lấy theo sự kiện
        List<FeedbackDTO> feedbacks = btcService.getFeedbacks(btcId, eventId);

        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("totalReviews", feedbacks.size());

        // 3. Tính toán Stats dựa trên List vừa lấy (Dynamic calculation)
        double avg = 0.0;

        if (!feedbacks.isEmpty()) {
            // Cộng tổng rating từ danh sách hiện tại
            double sum = feedbacks.stream()
                    .mapToDouble(FeedbackDTO::getRating) // Giả sử rating là Integer hoặc Double
                    .sum();

            // Chia trung bình
            avg = sum / feedbacks.size();

            // Làm tròn 1 chữ số thập phân (VD: 4.666 -> 4.7)
            avg = Math.round(avg * 10.0) / 10.0;
        }

        model.addAttribute("avgRating", avg);

        // 4. Logic vẽ sao (Dựa trên avg vừa tính lại)
        int fullStars = (int) avg;
        boolean hasHalfStar = (avg - fullStars) >= 0.5;
        int emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        model.addAttribute("fullStars", fullStars);
        model.addAttribute("hasHalfStar", hasHalfStar);
        model.addAttribute("emptyStars", emptyStars);

        model.addAttribute("activeTab", "feedback");
        return "btc/feedback";
    }

    @PostMapping("/reply")
    public String submitReply(@AuthenticationPrincipal CustomUserDetails user,
                              @RequestParam Long feedbackId,
                              @RequestParam String replyContent,
                              @RequestParam(required = false) Integer currentEventId) { // Để redirect về đúng chỗ

        btcService.replyToFeedback(feedbackId, replyContent, user.getUser().getId());

        // Redirect giữ lại filter event nếu có
        return "redirect:/btc/feedback" + (currentEventId != null ? "?eventId=" + currentEventId : "");
    }
}