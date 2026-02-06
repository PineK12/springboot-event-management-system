package com.example.vadoo.controller.student;

import com.example.vadoo.dto.student.StudentEventDTO;
import com.example.vadoo.dto.student.TicketDTO;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.student.StudentDashboardService;
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

import java.util.List;

@Controller
@RequestMapping("/student/events")
@RequiredArgsConstructor
public class StudentEventController {

    private final StudentDashboardService studentService;
    private final StudentEventService studentEventService;

    @GetMapping
    public String listEvents(@AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false, defaultValue = "all") String topic,
                             @RequestParam(required = false, defaultValue = "all") String status,
                             Model model) {

        Integer studentId = user.getUser().getId();

        // 1. Lấy danh sách đơn vị cho Filter
        List<String> donViList = studentService.getAllDonViNames();
        model.addAttribute("donViList", donViList);

        // 1. Gọi Service lấy danh sách
        List<StudentEventDTO> events = studentService.searchEvents(studentId, keyword, topic, status);

        // 2. Đẩy dữ liệu ra View
        model.addAttribute("events", events);
        model.addAttribute("user", user);

        // 3. Giữ lại giá trị bộ lọc để hiển thị trên UI (giữ trạng thái input/select)
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentTopic", topic);
        model.addAttribute("currentStatus", status);
        model.addAttribute("activeTab", "events"); // Để sáng menu Sidebar

        int count = studentService.countUpcomingTickets(studentId); // studentService ở đây là StudentDashboardService
        model.addAttribute("upcomingCount", count);

        return "student/events"; // Trả về file HTML Thymeleaf
    }

    // --- LOGIC XỬ LÝ ĐĂNG KÝ (Form Submit) ---
    @PostMapping("/register")
    public String registerEvent(@RequestParam Integer eventId,
                                @AuthenticationPrincipal CustomUserDetails user,
                                RedirectAttributes redirectAttributes) {
        try {
            // Gọi Service xử lý và lấy chuỗi QR
            String qrData = studentEventService.registerEvent(user.getUser().getId(), eventId);

            // Lưu dữ liệu vào Flash Attribute (Dữ liệu này sống sót qua lệnh redirect)
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công!");
            redirectAttributes.addFlashAttribute("qrData", qrData); // Gửi mã QR về View

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // Load lại trang danh sách
        return "redirect:/student/events";
    }

    @PostMapping("/cancel")
    public String cancelEvent(@RequestParam Integer eventId,
                              @AuthenticationPrincipal CustomUserDetails user,
                              RedirectAttributes redirectAttributes) {
        try {
            studentEventService.cancelEventRegistration(user.getUser().getId(), eventId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đăng ký thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/student/events";
    }

}