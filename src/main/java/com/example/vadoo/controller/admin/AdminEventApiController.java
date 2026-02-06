package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.admin.EventDTO;
import com.example.vadoo.security.CustomUserDetails; // Import cái này
import com.example.vadoo.service.admin.AdminEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Import cái này
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/api/events")
@RequiredArgsConstructor
public class AdminEventApiController {

    private final AdminEventService adminEventService;

    // API Approve Event
    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveEvent(@PathVariable Integer id,
                                          @AuthenticationPrincipal CustomUserDetails user) { // <--- THÊM DÒNG NÀY
        try {
            // SỬA DÒNG NÀY: Truyền thêm username vào tham số thứ 2
            adminEventService.approveEvent(id, user.getUsername());

            return ResponseEntity.ok().body("{\"success\": true}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // API Get Detail for Modal (Giữ nguyên)
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getEventDetail(@PathVariable Integer id) {
        try {
            EventDTO event = adminEventService.getEventDetail(id);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // API Từ chối sự kiện
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectEvent(@PathVariable Integer id,
                                         @RequestBody Map<String, String> payload, // Nhận JSON { "reason": "..." }
                                         @AuthenticationPrincipal CustomUserDetails user) {
        try {
            String reason = payload.get("reason");

            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Vui lòng nhập lý do từ chối\"}");
            }

            adminEventService.rejectEvent(id, reason, user.getUsername());

            return ResponseEntity.ok().body("{\"success\": true}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}