package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.admin.LogDetailDTO;
import com.example.vadoo.dto.admin.LogFilterDTO;
import com.example.vadoo.dto.admin.SystemLogDTO;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.UserRepository;
import com.example.vadoo.service.admin.AdminExcelExportService;
import com.example.vadoo.service.admin.AdminLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
@Slf4j
public class LogsController {

    private final AdminLogService adminLogService;
    private final UserRepository userRepository;
    @Autowired
    private AdminExcelExportService adminExcelExportService;

    // ========== TRANG CHÍNH ==========

    @GetMapping
    public String logsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "all") String actionType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            Authentication authentication,
            Model model) {

        try {
            // 1. User info
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithDetails(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("displayName", user.getHoTen());

            // 2. Build filter
            LogFilterDTO filter = LogFilterDTO.builder()
                    .keyword(keyword)
                    .startDate(startDate)
                    .endDate(endDate)
                    .actionType(actionType)
                    .page(page)
                    .size(size)
                    .build();

            // 3. Get logs
            Page<SystemLogDTO> logs = adminLogService.searchLogs(filter);

            model.addAttribute("logs", logs.getContent());
            model.addAttribute("currentPage", logs.getNumber());
            model.addAttribute("totalPages", logs.getTotalPages());
            model.addAttribute("totalElements", logs.getTotalElements());
            model.addAttribute("pageSize", logs.getSize());

            // 4. Filter params (giữ lại trạng thái filter khi reload trang)
            model.addAttribute("keyword", keyword);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("actionType", actionType);

            // 5. Stats
            Map<String, Long> stats = adminLogService.getLogStats();
            model.addAttribute("stats", stats);

            // ============================================================
            // ✅ PHẦN THÊM MỚI: TẠO DANH SÁCH HÀNH ĐỘNG CHO DROPDOWN
            // ============================================================
            // Sử dụng LinkedHashMap để giữ thứ tự chèn (Create -> Update -> Delete...)
            Map<String, String> actionList = new LinkedHashMap<>();
            actionList.put("CREATE", "➕ Tạo mới (Create)");
            actionList.put("UPDATE", "✏️ Chỉnh sửa (Update)");
            actionList.put("DELETE", "🗑️ Xóa (Delete)");
            actionList.put("LOGIN", "🔑 Đăng nhập (Auth)");
            actionList.put("RESET_PASSWORD", "🔐 Đặt lại mật khẩu (Reset Pass)");
            actionList.put("APPROVE", "✅ Phê duyệt (Approve)");
            actionList.put("REJECT", "❌ Từ chối (Reject)");
            actionList.put("CANCEL", "🚫 Hủy bỏ (Cancel)");
            actionList.put("TOGGLE", "🔄 Đổi trạng thái (Toggle)"); // Dùng cho điểm danh

            model.addAttribute("actionList", actionList);
            // ============================================================

            return "admin/logs";

        } catch (Exception e) {
            log.error("Error loading logs page", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "error";
        }
    }

    // ========== API ENDPOINTS ==========

    /**
     * API: Lấy danh sách logs (AJAX)
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<Page<SystemLogDTO>> getLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "all") String actionType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        try {
            LogFilterDTO filter = LogFilterDTO.builder()
                    .keyword(keyword)
                    .startDate(startDate)
                    .endDate(endDate)
                    .actionType(actionType)
                    .page(page)
                    .size(size)
                    .build();

            Page<SystemLogDTO> logs = adminLogService.searchLogs(filter);
            return ResponseEntity.ok(logs);

        } catch (Exception e) {
            log.error("Error getting logs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API: Lấy chi tiết 1 log
     */
    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public ResponseEntity<LogDetailDTO> getLogDetail(@PathVariable Long id) {
        try {
            LogDetailDTO detail = adminLogService.getLogDetail(id);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("Error getting log detail: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * API: Lấy thống kê
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getStats() {
        try {
            Map<String, Long> stats = adminLogService.getLogStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API: Xuất logs (TODO)
     */
    @GetMapping("/api/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "all") String actionType) {

        try {
            // 1. Lấy dữ liệu theo filter (Lấy tất cả, không phân trang -> size lớn)
            LogFilterDTO filter = LogFilterDTO.builder()
                    .keyword(keyword)
                    .startDate(startDate)
                    .endDate(endDate)
                    .actionType(actionType)
                    .page(0)
                    .size(10000) // Giới hạn 10,000 dòng để tránh treo server
                    .build();

            Page<SystemLogDTO> logsPage = adminLogService.searchLogs(filter);
            List<SystemLogDTO> logs = logsPage.getContent();

            // 2. Tạo file Excel
            byte[] excelContent = adminExcelExportService.exportLogsToExcel(logs);

            // 3. Tên file: SystemLogs_20241210_1530.xlsx
            String fileName = "SystemLogs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);

        } catch (Exception e) {
            log.error("Error exporting logs", e);
            return ResponseEntity.internalServerError().build();
        }
    }


}