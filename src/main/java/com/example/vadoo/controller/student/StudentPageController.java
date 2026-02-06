package com.example.vadoo.controller.student;

import com.example.vadoo.dto.student.StudentDashboardDTO;
import com.example.vadoo.dto.student.TicketDTO;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.student.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentPageController {

    private final StudentDashboardService dashboardService;


    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Integer studentId = user.getUser().getId();

        // Lấy dữ liệu tổng quan
        StudentDashboardDTO data = dashboardService.getDashboardData(studentId);

        model.addAttribute("data", data);
        model.addAttribute("user", user);
        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("upcomingCount", data.getSoSuKienSapToi());

        return "student/dashboard"; // File HTML chúng ta sẽ tạo
    }

    @GetMapping("/my-qr")
    public String myTickets(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Integer studentId = user.getUser().getId();

        List<TicketDTO> tickets = dashboardService.getStudentTickets(studentId);

        // Tách ra 2 list: Sắp tới và Lịch sử
        List<TicketDTO> upcomingTickets = tickets.stream().filter(t -> !t.isPast()).toList();
        List<TicketDTO> pastTickets = tickets.stream().filter(t -> t.isPast()).toList();

        model.addAttribute("upcomingTickets", upcomingTickets);
        model.addAttribute("pastTickets", pastTickets);
        model.addAttribute("user", user);
        model.addAttribute("activeTab", "my-qr");
        model.addAttribute("upcomingCount", upcomingTickets.size());

        return "student/my-qr";
    }
}