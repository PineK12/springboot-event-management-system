package com.example.vadoo.controller.student;

import com.example.vadoo.dto.student.StudentProfileDTO;
import com.example.vadoo.entity.SinhVien;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.student.StudentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentEventService studentService;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        StudentProfileDTO profile = studentService.getProfile(user.getUser().getId());
        model.addAttribute("profile", profile);
        model.addAttribute("user", user);
        model.addAttribute("activeTab", "profile");
        return "student/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails user,
                                @ModelAttribute StudentProfileDTO dto,
                                RedirectAttributes redirectAttributes) {
        try {
            studentService.updateProfile(user.getUser().getId(), dto);

            // --- CẬP NHẬT SESSION (Security Context) ---
            // 1. Lấy lại thông tin mới nhất từ DB để đảm bảo chính xác
            SinhVien svUpdated = studentService.getSinhVienById(user.getUser().getId());

            // 2. Cập nhật vào User đang đăng nhập (để Header hiển thị đúng)
            user.getUser().setAvatarUrl(dto.getAvatarUrl());
            // Lưu ý: User không có setDisplayName, nhưng getHoTen() sẽ tự lấy từ SinhVien
            // Vì vậy ta cần cập nhật SinhVien trong object User này
            user.getUser().setSinhVien(svUpdated);

            // 3. Set lại Authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Authentication newAuth = new UsernamePasswordAuthenticationToken(user, auth.getCredentials(), auth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails user,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            studentService.changePassword(user.getUser().getId(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/profile";
    }
}