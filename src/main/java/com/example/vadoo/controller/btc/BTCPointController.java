package com.example.vadoo.controller.btc;

import com.example.vadoo.dto.btc.PointHistoryDTO;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.btc.BTCDashboardService;
import com.example.vadoo.service.btc.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/btc/points")
@RequiredArgsConstructor
public class BTCPointController {

    private final BTCDashboardService btcService;
    private final ExcelExportService excelService;

    @GetMapping
    public String viewPoints(@AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false, defaultValue = "2025-1") String semester,
                             Model model) {
        Integer btcId = user.getUser().getId();

        // 1. Sidebar Stats & User Info
        model.addAttribute("stats", btcService.getDashboardStats(btcId));
        model.addAttribute("user", user);

        // 2. Data Stats (Card trên cùng)
        int totalPoints = btcService.getTotalPointsGiven(btcId);
        model.addAttribute("totalPointsGiven", totalPoints);

        // 3. Table Data
        List<PointHistoryDTO> history = btcService.getPointHistory(btcId, keyword, semester);
        model.addAttribute("history", history);
        model.addAttribute("keyword", keyword);

        // --- TÍNH ĐIỂM TRUNG BÌNH (MỚI THÊM) ---
        double avgPoints = 0.0;
        if (!history.isEmpty()) {
            avgPoints = (double) totalPoints / history.size();
            // Làm tròn 1 chữ số thập phân (VD: 5.5)
            avgPoints = Math.round(avgPoints * 10.0) / 10.0;
        }
        model.addAttribute("avgPoints", avgPoints);
        // ---------------------------------------

        // 4. Active Sidebar
        model.addAttribute("activeTab", "points");

        // 1. Tạo danh sách Học kỳ (Giả lập)
        // Trong thực tế, bạn nên có bảng 'HocKy' trong DB
        Map<String, String> semesters = new LinkedHashMap<>();
        semesters.put("2025-1", "Học kỳ I (2025-2026)");
        semesters.put("2024-3", "Học kỳ Hè (2024-2025)");
        semesters.put("2024-2", "Học kỳ II (2024-2025)");
        semesters.put("ALL", "Tất cả thời gian");

        model.addAttribute("semesters", semesters);
        model.addAttribute("currentSemester", semester);

        // 2. Gọi Service (Có thể truyền thêm semester vào hàm getPointHistory nếu muốn lọc thật)
        // Hiện tại tạm thời chưa lọc DB theo ngày, chỉ giữ param để UI hiển thị đúng
//        List<PointHistoryDTO> history = btcService.getPointHistory(btcId, keyword);

        model.addAttribute("history", history);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeTab", "points");

        return "btc/points";
    }

    /**
     * API Xuất Excel Lịch sử điểm
     */
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportExcel(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false, defaultValue = "2025-1") String semester) {
        Integer btcId = user.getUser().getId();

        // 1. Lấy dữ liệu (Dùng đúng logic lọc giống hệt trang xem)
        List<PointHistoryDTO> history = btcService.getPointHistory(btcId, keyword, semester);

        // 2. Tạo file
        ByteArrayInputStream in = excelService.exportPointHistory(history, semester);

        // 3. Trả về
        String fileName = "Bao_cao_diem_" + semester + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + fileName);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }
}