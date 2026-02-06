package com.example.vadoo.controller.btc;

import com.example.vadoo.dto.btc.BTCEventSummaryDTO;
import com.example.vadoo.dto.btc.RegistrationDTO;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.repository.SuKienRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/btc/registrations")
@RequiredArgsConstructor
public class BTCRegistrationController {

    private final BTCDashboardService btcService;
    private final SuKienRepository suKienRepository;
    private final ExcelExportService excelExportService;

    @GetMapping
    public String viewRegistrations(@AuthenticationPrincipal CustomUserDetails user,
                                    @RequestParam(required = false) Integer eventId,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false, defaultValue = "ALL") String status, // Thêm
                                    Model model) {
        Integer btcId = user.getUser().getId();

        // 1. Sidebar Stats
        model.addAttribute("stats", btcService.getDashboardStats(btcId));

        // 2. Danh sách sự kiện để hiển thị Dropdown lọc
        // Lấy tất cả sự kiện của BTC này (kể cả đã kết thúc)
        List<SuKien> allEvents = suKienRepository.findByBtcIdOrderByIdDesc(btcId); // Cần thêm hàm này vào Repo
        model.addAttribute("events", allEvents);

        // 3. Nếu chưa chọn event nào -> Chọn cái mới nhất làm mặc định
        if (eventId == null && !allEvents.isEmpty()) {
            eventId = allEvents.get(0).getId();
        }

        // 4. Lấy danh sách sinh viên
        List<RegistrationDTO> students = btcService.getRegistrations(eventId, keyword, status);

        model.addAttribute("user", user);
        model.addAttribute("students", students);
        model.addAttribute("currentEventId", eventId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeTab", "registrations");

        return "btc/registrations";
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportExcel(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam Integer eventId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false, defaultValue = "ALL") String status) {
        // 1. Lấy dữ liệu (Tái sử dụng logic cũ)
        List<RegistrationDTO> students = btcService.getRegistrations(eventId, keyword, status);

        // Lấy tên sự kiện để đặt tên file (Optional)
        String eventName = "Event_" + eventId;

        // 2. Tạo file Excel
        ByteArrayInputStream in = excelExportService.exportRegistrationsToExcel(students, eventName);

        // 3. Trả về file
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Danh_sach_tham_gia_" + eventId + ".xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }
}