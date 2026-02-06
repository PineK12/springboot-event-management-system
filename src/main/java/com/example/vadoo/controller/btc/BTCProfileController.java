package com.example.vadoo.controller.btc;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/btc/profile")
@RequiredArgsConstructor
public class BTCProfileController {

    private final BTCDashboardService btcService;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Integer btcId = user.getUser().getId();

        model.addAttribute("user", user);
        model.addAttribute("stats", btcService.getDashboardStats(btcId)); // Để hiển thị Sidebar
        model.addAttribute("activeTab", "profile");

        return "btc/profile";
    }

    // Cập nhật thông tin cơ bản
    @PostMapping("/update")
    public String updateInfo(@AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam String sdt,
                             @RequestParam String avatarUrl,
                             RedirectAttributes redirectAttributes) {
        try {
            btcService.updateProfile(user.getUser().getId(), sdt, avatarUrl);
            // Cập nhật lại thông tin trong session (để hiển thị ngay lập tức)
            user.getUser().setSdt(sdt);
            user.getUser().setAvatarUrl(avatarUrl);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/btc/profile";
    }

    // Đổi mật khẩu
    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorPass", "Mật khẩu xác nhận không khớp!");
            return "redirect:/btc/profile";
        }

        boolean success = btcService.changePassword(user.getUser().getId(), oldPassword, newPassword);

        if (success) {
            redirectAttributes.addFlashAttribute("successPass", "Đổi mật khẩu thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorPass", "Mật khẩu cũ không chính xác!");
        }

        return "redirect:/btc/profile";
    }
}