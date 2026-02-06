package com.example.vadoo.controller.student;

import com.example.vadoo.dto.student.StudentHistoryDTO;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.student.StudentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student/history")
@RequiredArgsConstructor
public class StudentHistoryController {

    private final StudentEventService studentService;

    @GetMapping
    public String viewHistory(@AuthenticationPrincipal CustomUserDetails user,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false, defaultValue = "2025-1") String semester,
                              @RequestParam(required = false, defaultValue = "ALL") String status,
                              Model model) {
        Integer userId = user.getUser().getId();

        // 1. Stats (Giữ nguyên)
        Map<String, Object> stats = studentService.getStudentStats(userId);
        model.addAttribute("stats", stats);

        // 2. List History (CÓ FILTER)
        // Gọi hàm mới trong Service nhận đủ 3 tham số lọc
        List<StudentHistoryDTO> history = studentService.getStudentHistory(userId, keyword, semester, status);
        model.addAttribute("history", history);

        // 3. Data cho bộ lọc (Giữ trạng thái selected)
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentSemester", semester);
        model.addAttribute("currentStatus", status);

        // Map Học kỳ (Nên đưa ra 1 class Utils dùng chung, nhưng tạm thời để đây)
        Map<String, String> semesters = new LinkedHashMap<>();
        semesters.put("2025-1", "Học kỳ I (2025-2026)");
        semesters.put("2024-3", "Học kỳ Hè (2024-2025)");
        semesters.put("2024-2", "Học kỳ II (2024-2025)");
        semesters.put("ALL", "Tất cả thời gian");
        model.addAttribute("semesters", semesters);

        model.addAttribute("user", user);
        model.addAttribute("activeTab", "history");

        return "student/history";
    }

    @PostMapping("/rate")
    public String submitRating(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam Long registrationId,
                               @RequestParam Integer rating,
                               @RequestParam String comment,
                               RedirectAttributes redirectAttributes) {
        try {
            studentService.submitRating(user.getUser().getId(), registrationId, rating, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã đánh giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/student/history";
    }
}