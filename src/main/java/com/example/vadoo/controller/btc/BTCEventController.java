package com.example.vadoo.controller.btc;

import com.example.vadoo.dto.btc.BTCEventCreateDTO;
import com.example.vadoo.dto.btc.BTCEventDetailDTO;
import com.example.vadoo.entity.DangKySuKien;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.repository.DangKySuKienRepository;
import com.example.vadoo.repository.SuKienRepository;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.btc.BTCEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/btc/events")
@RequiredArgsConstructor
public class BTCEventController {

    private final BTCEventService btcEventService;
    private final SuKienRepository suKienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;

    // 1. Trang chi tiết sự kiện
    // Xem chi tiết sự kiện
    @GetMapping("/{id}")
    public String eventDetail(@PathVariable Integer id,
                              @AuthenticationPrincipal CustomUserDetails user,
                              Model model) {
        try {
            // Lấy ID thật của BTC
            Integer btcId = user.getUser().getId();

            BTCEventDetailDTO event = btcEventService.getEventDetail(id, btcId);

            model.addAttribute("event", event);
            model.addAttribute("user", user);
            model.addAttribute("activeTab", "events"); // Active menu Sự kiện

            return "btc/event-detail";
        } catch (Exception e) {
            return "redirect:/btc/dashboard?error=not_found";
        }
    }

    //3.
    @GetMapping
    public String listEvents(@AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false, defaultValue = "all") String status,
                             Model model) {

        Integer btcId = user.getUser().getId();

        // Xử lý Status (Từ String sang Enum)
        SuKien.TrangThaiSuKien enumStatus = null;
        if (status != null && !status.equals("all")) {
            try {
                enumStatus = SuKien.TrangThaiSuKien.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Nếu status rác thì mặc định là null (lấy tất cả)
                enumStatus = null;
            }
        }

        // Gọi Repo tìm kiếm
        List<SuKien> listEvents = suKienRepository.searchEventsByBtc(btcId, keyword, enumStatus);

        // Đẩy dữ liệu ra View
        model.addAttribute("events", listEvents);
        model.addAttribute("user", user);
        model.addAttribute("activeTab", "events");

        // Giữ lại giá trị filter để hiển thị trên giao diện (UX)
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentStatus", status);

        return "btc/events";
    }

    @PostMapping("/create")
    public String createEvent(@Valid @ModelAttribute BTCEventCreateDTO dto,
                              BindingResult result, // Chứa kết quả validate (@NotNull, @NotBlank...)
                              @AuthenticationPrincipal CustomUserDetails user,
                              RedirectAttributes redirectAttributes) {

        // 1. Nếu lỗi Validate cơ bản (trống, sai định dạng)
        if (result.hasErrors()) {
            // Lấy lỗi đầu tiên để hiển thị cho gọn
            String errorMsg = result.getFieldError().getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/btc/events";
        }

        // 2. Nếu lỗi Logic (Ngày tháng...)
        try {
            btcEventService.createEvent(dto, user.getUser().getId());
            redirectAttributes.addFlashAttribute("successMessage", "Gửi yêu cầu tạo sự kiện thành công! Vui lòng chờ Admin duyệt.");
        } catch (IllegalArgumentException e) {
            // Lỗi do logic ngày tháng không hợp lệ
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Lỗi hệ thống khác
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
        }

        return "redirect:/btc/events";
    }

    // API Xử lý tạo mới bằng AJAX
    @PostMapping("/api/create")
    @ResponseBody // Quan trọng: Trả về JSON
    public ResponseEntity<?> createEventAjax(@RequestBody @Valid BTCEventCreateDTO dto,
                                             BindingResult result,
                                             @AuthenticationPrincipal CustomUserDetails user) {

        // 1. Kiểm tra lỗi Validate (@NotNull, @NotBlank...)
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thông tin nhập chưa hợp lệ",
                    "fieldErrors", errors
            ));
        }

        // 2. Kiểm tra lỗi Logic (Service ném ra)
        try {
            btcEventService.createEvent(dto, user.getUser().getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo sự kiện thành công! Đang chờ Admin duyệt."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() // Lỗi logic ngày tháng
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + e.getMessage()
            ));
        }
    }

    // 1. API lấy dữ liệu sự kiện để điền vào Form sửa
    @GetMapping("/api/get/{id}")
    @ResponseBody
    public ResponseEntity<?> getEventForEdit(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(btcEventService.getEventForEdit(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 2. API Lưu cập nhật
    @PostMapping("/api/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateEvent(@PathVariable Integer id,
                                         @RequestBody @Valid BTCEventCreateDTO dto,
                                         BindingResult result,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            // ... (Copy logic xử lý lỗi giống hàm create)
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Dữ liệu không hợp lệ", "fieldErrors", errors));
        }

        try {
            btcEventService.updateEvent(id, dto, user.getUser().getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // API Xóa sự kiện
    @DeleteMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteEvent(@PathVariable Integer id,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        try {
            btcEventService.deleteEvent(id, user.getUser().getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa sự kiện thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/checkin")
    public String showCheckinPage(@PathVariable Integer id,
                                  Model model,
                                  @AuthenticationPrincipal CustomUserDetails userDetails) {

        // --- 1. QUAN TRỌNG: Truyền thông tin User để hiển thị Header/Sidebar ---
        if (userDetails != null) {
            // Truyền CustomUserDetails xuống với tên 'user' để khớp với th:text="${user.displayName}"
            model.addAttribute("user", userDetails);
            // Hoặc nếu HTML dùng ${displayName} riêng:
            model.addAttribute("displayName", userDetails.getDisplayName());
        }

        // 2. Lấy thông tin sự kiện
        SuKien event = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        model.addAttribute("event", event);

        // 3. Lấy thống kê
        long totalRegistered = dangKySuKienRepository.countBySuKienAndTrangThai(
                event, DangKySuKien.TrangThaiDangKy.REGISTERED);

        // Đếm số người đã check-in (Trạng thái PRESENT)
        long countCheckedIn = dangKySuKienRepository.countBySuKienAndTrangthaiThamgia(
                event, DangKySuKien.TrangThaiThamGia.PRESENT);

        model.addAttribute("totalRegistered", totalRegistered);
        model.addAttribute("countCheckedIn", countCheckedIn);

        // 4. List log checkin (để trống tạm thời để tránh lỗi null list)
        model.addAttribute("recentCheckins", new ArrayList<>());

        // Trả về đúng tên file HTML bạn đã tạo
        // Nếu file của bạn nằm ở src/main/resources/templates/btc/checkin.html
        return "btc/checkin";
    }
}