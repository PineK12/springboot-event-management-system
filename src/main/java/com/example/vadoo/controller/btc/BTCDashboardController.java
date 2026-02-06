package com.example.vadoo.controller.btc;

import com.example.vadoo.dto.btc.BTCEventSummaryDTO;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.btc.BTCDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/btc")
@RequiredArgsConstructor
public class BTCDashboardController {

    private final BTCDashboardService btcDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Integer userId = user.getUser().getId();

        // 1. Lấy thống kê (Code cũ)
        Map<String, Object> stats = btcDashboardService.getDashboardStats(userId);
        model.addAttribute("stats", stats);

        // 2. Lấy danh sách sự kiện sắp diễn ra (MỚI THÊM)
        // Lấy 5 sự kiện gần nhất
        List<BTCEventSummaryDTO> upcomingEvents = btcDashboardService.getUpcomingEvents(userId, 5);
        model.addAttribute("upcomingEvents", upcomingEvents);

        // 3. Các thông tin chung
        model.addAttribute("user", user);
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("activeTab", "dashboard"); // Để highlight menu

        return "btc/dashboard";
    }
}