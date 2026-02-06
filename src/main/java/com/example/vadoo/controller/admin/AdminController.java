package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.admin.DashboardStatsDTO;
import com.example.vadoo.dto.admin.PendingEventDTO;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("Admin {} accessing dashboard", userDetails.getUsername());

        // Thông tin user
        model.addAttribute("user", userDetails);
        model.addAttribute("displayName", userDetails.getDisplayName());
        model.addAttribute("roleName", userDetails.getRoleName());

        // Thống kê dashboard
        DashboardStatsDTO stats = adminDashboardService.getDashboardStats();
        model.addAttribute("stats", stats);

        // Danh sách sự kiện chờ duyệt
        List<PendingEventDTO> pendingEvents = adminDashboardService.getRecentPendingEvents();
        model.addAttribute("pendingEvents", pendingEvents);

        log.info("Dashboard stats: Users={}, Active={}, Pending={}, Points={}",
                stats.getTotalUsers(),
                stats.getActiveEvents(),
                stats.getPendingEvents(),
                stats.getTotalPoints());

        return "admin/dashboard";
    }
}