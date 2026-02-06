package com.example.vadoo.controller.student;

import com.example.vadoo.dto.student.StudentHistoryDTO;
import com.example.vadoo.dto.student.StudentPointStatsDTO;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.student.StudentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student/points")
@RequiredArgsConstructor
public class StudentPointController {

    private final StudentEventService studentService;

    @GetMapping
    public String viewPoints(@AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam(required = false, defaultValue = "2025-1") String semester,
                             Model model) {

        // 1. Lấy thống kê chi tiết
        StudentPointStatsDTO stats = studentService.getPointStats(user.getUser().getId(), semester);
        model.addAttribute("pointStats", stats);

        // 2. Lấy danh sách chi tiết (để hiện bên dưới)
        List<StudentHistoryDTO> history = studentService.getStudentHistory(user.getUser().getId(), null, semester, "ALL");
        model.addAttribute("history", history);

        // 3. Filter Semester
        Map<String, String> semesters = new LinkedHashMap<>();
        semesters.put("2025-1", "Học kỳ I (2025-2026)");
        semesters.put("2024-2", "Học kỳ II (2024-2025)");
        model.addAttribute("semesters", semesters);
        model.addAttribute("currentSemester", semester);

        model.addAttribute("user", user);
        model.addAttribute("activeTab", "points");

        return "student/points";
    }
}