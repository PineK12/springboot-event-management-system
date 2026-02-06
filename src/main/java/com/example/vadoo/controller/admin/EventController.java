package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.admin.EventDTO;
import com.example.vadoo.dto.admin.EventRequestDTO;
import com.example.vadoo.dto.admin.RejectRequestDTO;
import com.example.vadoo.entity.Btc;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.BtcRepository;
import com.example.vadoo.repository.UserRepository;
import com.example.vadoo.security.CustomUserDetails; // ✅ Import class này
import com.example.vadoo.service.admin.AdminEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventController {

    private final AdminEventService adminEventService;
    private final BtcRepository btcRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String eventsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String statusFilter,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();
        User userWithDetails = userRepository.findByUsernameWithDetails(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", userWithDetails);
        model.addAttribute("displayName", userWithDetails.getHoTen());

        List<EventDTO> events;
        if (keyword != null && !keyword.isEmpty()) {
            events = adminEventService.searchEvents(keyword, statusFilter);
        } else if (statusFilter != null && !statusFilter.equals("all")) {
            events = adminEventService.searchEvents(null, statusFilter);
        } else {
            events = adminEventService.getAllEvents();
        }

        List<Btc> btcList = btcRepository.findAll();

        model.addAttribute("events", events);
        model.addAttribute("btcList", btcList);
        model.addAttribute("totalEvents", adminEventService.getTotalEvents());
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", statusFilter);

        return "admin/events";
    }

    // --- API endpoints ---

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<EventDTO> getEvent(@PathVariable Integer id) {
        try {
            EventDTO event = adminEventService.getEventById(id);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventRequestDTO request, Authentication authentication) {
        try {
            // 1. Lấy Principal và ép kiểu về CustomUserDetails
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // 2. Lấy Entity User từ bên trong nó
            User currentUser = userDetails.getUser();

            // 3. Truyền vào Service
            EventDTO createdEvent = adminEventService.createEvent(request, currentUser.getUsername());

            return ResponseEntity.ok(createdEvent); // Trả về object vừa tạo
        } catch (Exception e) {
            e.printStackTrace(); // In lỗi ra console để debug
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable Integer id,
            @RequestBody EventRequestDTO request) {
        try {
            EventDTO updated = adminEventService.updateEvent(id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<Void> approveEvent(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            // ✅ SỬA LỖI ÉP KIỂU
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            adminEventService.approveEvent(id, user.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<Void> rejectEvent(
            @PathVariable Integer id,
            @RequestBody RejectRequestDTO request,
            Authentication authentication) {
        try {
            // ✅ SỬA LỖI ÉP KIỂU
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            adminEventService.rejectEvent(id, request.getReason(), user.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancelEvent(@PathVariable Integer id, Authentication authentication) {
        try {
            // ✅ SỬA LỖI ÉP KIỂU & Thêm lấy user để biết ai hủy
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            adminEventService.cancelEvent(id, user.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ THÊM API XÓA (DELETE)
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteEvent(@PathVariable Integer id) {
        try {
            adminEventService.deleteEvent(id);
            return ResponseEntity.ok("Xóa thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi khi xóa: " + e.getMessage());
        }
    }
}