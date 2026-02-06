package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.admin.EventStatsDTO;
import com.example.vadoo.dto.admin.StatsDTO;
import com.example.vadoo.dto.admin.StudentAttendanceDTO;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.UserRepository;
import com.example.vadoo.service.admin.AdminExcelExportService;
import com.example.vadoo.service.admin.AdminStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final AdminStatsService adminStatsService;
    private final UserRepository userRepository;

    private final AdminExcelExportService adminExcelExportService;

    // ========== TRANG CHÍNH ==========

    @GetMapping
    public String statsPage(
            @RequestParam(required = false, defaultValue = "all") String filter,
            Authentication authentication,
            Model model) {

        try {
            // Get user info
            String username = authentication.getName();
            User userWithDetails = userRepository.findByUsernameWithDetails(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("user", userWithDetails);
            model.addAttribute("displayName", userWithDetails.getHoTen());

            // Get overall statistics
            StatsDTO stats = adminStatsService.getOverallStats();
            model.addAttribute("stats", stats);

            // Get events list
            List<EventStatsDTO> events;
            if ("all".equals(filter)) {
                events = adminStatsService.getAllEventStats();
            } else {
                events = adminStatsService.getEventStatsByFilter(filter);
            }
            model.addAttribute("events", events);
            model.addAttribute("currentFilter", filter);

            log.info("Stats page loaded for user: {}, filter: {}, events count: {}",
                    username, filter, events.size());

            return "admin/stats";

        } catch (Exception e) {
            log.error("Error loading stats page", e);
            model.addAttribute("error", "Lỗi khi tải trang thống kê: " + e.getMessage());
            return "error";
        }
    }

    // ========== API ENDPOINTS ==========

    /**
     * Xuất danh sách sinh viên ra Excel
     */
    @GetMapping("/api/events/{id}/students/export")
    public ResponseEntity<byte[]> exportStudentsToExcel(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "all") String filter) {
        try {
            // Lấy danh sách sinh viên
            List<StudentAttendanceDTO> students;
            if ("all".equals(filter)) {
                students = adminStatsService.getStudentsBySuKien(id);
            } else {
                students = adminStatsService.getStudentsBySuKienAndFilter(id, filter);
            }

            // Lấy tên sự kiện
            EventStatsDTO event = adminStatsService.getEventById(id);
            String eventName = event != null ? event.getTenSuKien() : "SuKien";

            // Generate Excel file
            byte[] excelBytes = adminExcelExportService.exportStudentsToExcel(students, eventName);

            // ✅ TẠO TÊN FILE LOGIC
            String filename = generateFilename(eventName, filter);

            // Return file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            log.info("Exported Excel: {} with {} students", filename, students.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            log.error("Error exporting Excel for event {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ METHOD TẠO TÊN FILE
    private String generateFilename(String eventName, String filter) {
        // Sanitize tên sự kiện
        String cleanEventName = sanitizeFilename(eventName);

        // Giới hạn độ dài tên sự kiện (tối đa 50 ký tự)
        if (cleanEventName.length() > 50) {
            cleanEventName = cleanEventName.substring(0, 50);
        }

        // Lấy ngày giờ hiện tại
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
        String date = now.format(dateFormatter);
        String time = now.format(timeFormatter);

        // Tên filter
        String filterText = "";
        switch (filter) {
            case "attended":
                filterText = "_DaDiemDanh";
                break;
            case "not-attended":
                filterText = "_ChuaDiemDanh";
                break;
            default:
                filterText = "_TatCa";
                break;
        }

        // Format: DanhSach_TenSuKien_Filter_Ngay_Gio.xlsx
        return String.format("DanhSach_%s%s_%s_%s.xlsx",
                cleanEventName, filterText, date, time);
    }

    /**
     * Sanitize filename để tránh ký tự đặc biệt
     */
    private String sanitizeFilename(String filename) {
        // Loại bỏ dấu tiếng Việt và ký tự đặc biệt
        String normalized = java.text.Normalizer.normalize(filename, java.text.Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");
        // Chỉ giữ chữ, số, gạch ngang, gạch dưới
        return withoutAccents.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // Lấy thống kê tổng quan
    @GetMapping("/api/overview")
    @ResponseBody
    public ResponseEntity<StatsDTO> getOverviewStats() {
        try {
            StatsDTO stats = adminStatsService.getOverallStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting overview stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Lấy danh sách sự kiện theo filter
    @GetMapping("/api/events")
    @ResponseBody
    public ResponseEntity<List<EventStatsDTO>> getEventStats(
            @RequestParam(required = false, defaultValue = "all") String filter) {
        try {
            List<EventStatsDTO> events;
            if ("all".equals(filter)) {
                events = adminStatsService.getAllEventStats();
            } else {
                events = adminStatsService.getEventStatsByFilter(filter);
            }
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error getting event stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Lấy danh sách sinh viên của 1 sự kiện
    @GetMapping("/api/events/{id}/students")
    @ResponseBody
    public ResponseEntity<List<StudentAttendanceDTO>> getStudentsByEvent(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "all") String filter) {
        try {
            List<StudentAttendanceDTO> students;
            if ("all".equals(filter)) {
                students = adminStatsService.getStudentsBySuKien(id);
            } else {
                students = adminStatsService.getStudentsBySuKienAndFilter(id, filter);
            }
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error getting students for event {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Toggle điểm danh
    @PostMapping("/api/attendance/{dangKyId}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleAttendance(@PathVariable Long dangKyId) {
        try {
            adminStatsService.toggleAttendance(dangKyId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật điểm danh thành công"
            ));
        } catch (Exception e) {
            log.error("Error toggling attendance for registration {}", dangKyId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}