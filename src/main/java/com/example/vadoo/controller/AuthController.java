package com.example.vadoo.controller;

import com.example.vadoo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "expired", required = false) String expired,
                            Model model) {

        // Check if user is already authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String role = userDetails.getRoleName();

            return switch (role.toUpperCase()) {
                case "ADMIN" -> "redirect:/admin/dashboard";
                case "BTC" -> "redirect:/btc/dashboard";
                case "SINHVIEN" -> "redirect:/student/dashboard";
                default -> "redirect:/";
            };
        }

        if (error != null) {
            model.addAttribute("error", error);
        }

        if (logout != null) {
            model.addAttribute("message", "Đăng xuất thành công!");
        }

        if (expired != null) {
            model.addAttribute("error", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        return "auth/login";
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String role = userDetails.getRoleName();

            return switch (role.toUpperCase()) {
                case "ADMIN" -> "redirect:/admin/dashboard";
                case "BTC" -> "redirect:/btc/dashboard";
                case "SINHVIEN" -> "redirect:/student/dashboard";
                default -> "redirect:/login";
            };
        }
        return "redirect:/login";
    }
}