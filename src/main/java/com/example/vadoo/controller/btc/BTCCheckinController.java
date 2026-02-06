package com.example.vadoo.controller.btc;

import com.example.vadoo.dto.btc.BTCEventSummaryDTO;
import com.example.vadoo.entity.DangKySuKien;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.repository.DangKySuKienRepository;
import com.example.vadoo.repository.SuKienRepository;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.btc.BTCDashboardService;
import com.example.vadoo.service.btc.BTCEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/btc/checkin")
@RequiredArgsConstructor
public class BTCCheckinController {

    private final SuKienRepository suKienRepository;
    private final BTCDashboardService btcDashboardService;
    private final BTCEventService btcEventService;
    private final DangKySuKienRepository dangKySuKienRepository;

    // 2. Điểm danh
    @GetMapping
    public String checkinPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        Integer btcId = user.getUser().getId();

        // 1. Stats (Giữ nguyên)
        var stats = btcDashboardService.getDashboardStats(btcId);
        model.addAttribute("stats", stats);

        // 2. Lấy danh sách sự kiện ĐỂ CHECK-IN
        LocalDateTime now = LocalDateTime.now();

        // Định nghĩa: Cho phép check-in sớm trước 2 tiếng
        LocalDateTime checkinWindowOpen = now.plusHours(0);

        // Gọi Repository (Lấy các sự kiện APPROVED và Chưa kết thúc)
        List<SuKien> entities = suKienRepository.findEventsForCheckin(
                btcId,
                SuKien.TrangThaiSuKien.APPROVED, // <--- THÊM THAM SỐ NÀY
                now
        );

        // 3. Lọc và Map sang DTO
        List<BTCEventSummaryDTO> activeEvents = entities.stream()
                // LỌC: Chỉ lấy sự kiện đã bắt đầu HOẶC sắp bắt đầu trong 2h tới
                .filter(ev -> ev.getThoigianBatdau().isBefore(checkinWindowOpen))
                .map(ev -> {
                    // Logic xác định sự kiện có đang chạy thực sự không
                    boolean running = ev.getThoigianBatdau().isBefore(now)
                            && ev.getThoigianKetthuc().isAfter(now);

                    return BTCEventSummaryDTO.builder()
                            .eventId(ev.getId())
                            .tieuDe(ev.getTieuDe())
                            .thoigianBatdau(ev.getThoigianBatdau())
                            .thoigianKetthuc(ev.getThoigianKetthuc())
                            .isRunning(running) // True nếu đang trong giờ, False nếu check-in sớm
                            .build();
                })
                .collect(Collectors.toList());

        model.addAttribute("activeEvents", activeEvents);

        // Sửa lỗi hiển thị tên user trên header (nếu HTML dùng)
        model.addAttribute("user", user);
        model.addAttribute("displayName", user.getDisplayName());

        model.addAttribute("activeTab", "checkin");

        return "btc/checkin";
    }

    // --- THÊM HÀM NÀY ĐỂ XỬ LÝ QUÉT QR ---
    @PostMapping("/process")
    @ResponseBody // Báo cho Spring biết: Đừng tìm file HTML, hãy trả về dữ liệu thô (JSON)
    public Map<String, Object> processCheckIn(@RequestBody Map<String, String> payload) {
        String qrCode = payload.get("qrCode");
        String eventIdStr = payload.get("eventId");

        try {
            Integer eventId = Integer.parseInt(eventIdStr);
            // Gọi Service xử lý logic
            return btcEventService.processCheckIn(qrCode, eventId);
        } catch (Exception e) {
            return Map.of("status", "error", "message", "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/checkin")
    public String showCheckinPage(@PathVariable Integer id,
                                  Model model,
                                  @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 1. Đưa thông tin User đăng nhập vào Model (SỬA LỖI DISPLAYNAME NULL)
        if (userDetails != null) {
            model.addAttribute("user", userDetails.getUser());
            model.addAttribute("displayName", userDetails.getDisplayName());
        }

        // 2. Lấy thông tin sự kiện
        SuKien event = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + id));
        model.addAttribute("event", event);

        // 3. Lấy thống kê (Tổng đăng ký & Đã check-in) để hiển thị lên Header
        // Đếm tổng số vé đã đăng ký
        long totalRegistered = dangKySuKienRepository.countBySuKienAndTrangThai(
                event, DangKySuKien.TrangThaiDangKy.REGISTERED);

        // Đếm số người đã check-in (Trạng thái PRESENT)
        long countCheckedIn = dangKySuKienRepository.countBySuKienAndTrangthaiThamgia(
                event, DangKySuKien.TrangThaiThamGia.PRESENT);

        model.addAttribute("totalRegistered", totalRegistered);
        model.addAttribute("countCheckedIn", countCheckedIn);

        // 4. (Tùy chọn) Lấy danh sách check-in gần đây nếu bạn có Service
        // List<CheckinLogDTO> recentLogs = checkinService.getRecentCheckins(id);
        // model.addAttribute("recentCheckins", recentLogs);

        // Nếu chưa có service log thì truyền list rỗng để tránh lỗi HTML
        model.addAttribute("recentCheckins", new ArrayList<>());

        // Trả về file HTML: src/main/resources/templates/btc/checkin.html
        return "btc/checkin";
    }
}
