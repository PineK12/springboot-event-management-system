package com.example.vadoo.controller.btc;

import com.example.vadoo.dto.btc.BTCEventSummaryDTO;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.repository.SuKienRepository;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.btc.BTCDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/btc")
@RequiredArgsConstructor
public class BTCPageController {

    // Helper để nạp thông tin chung cho Header (User, Title...)
    private void addCommonAttributes(Model model, CustomUserDetails user, String pageTitle, String activeTab) {
        model.addAttribute("user", user);
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("activeTab", activeTab); // Biến này để tô màu Menu
        // Tạm thời hardcode tên đơn vị, sau này lấy từ DB
        model.addAttribute("donViName", "CLB Tin Học");
    }
}