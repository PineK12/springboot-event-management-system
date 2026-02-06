package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.admin.UserDTO;
import com.example.vadoo.entity.DonVi;
import com.example.vadoo.entity.Role;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.DonViRepository;
import com.example.vadoo.repository.RoleRepository;
import com.example.vadoo.repository.UserRepository;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.admin.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AdminUserService adminUserService;
    private final DonViRepository donViRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public String userManagement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String roleFilter,
            HttpServletRequest request,
            Model model) {

        log.info("Admin {} accessing user management page", userDetails.getUsername());

        String contextPath = request.getContextPath();
        model.addAttribute("contextPath", contextPath);
        model.addAttribute("user", userDetails);
        model.addAttribute("displayName", userDetails.getDisplayName());
        model.addAttribute("roleName", userDetails.getRoleName());

        Pageable pageable = PageRequest.of(page, size, Sort.by("thoigianTao").descending());
        Page<UserDTO> usersPage = adminUserService.searchUsers(keyword, roleFilter, pageable);

        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalUsers", usersPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleFilter", roleFilter);
        model.addAttribute("roles", adminUserService.getAllRoles());
        model.addAttribute("permissions", adminUserService.getAllPermissions());

        // --- PHẦN ĐÃ SỬA ĐỔI ---

        // 1. Dành cho BTC: Lấy TẤT CẢ đơn vị (Sắp xếp theo tên đầy đủ cho dễ tìm)
        // BTC có thể thuộc Khoa, Phòng ban, CLB... nên cần hiển thị hết.
        model.addAttribute("listAllDonVi", donViRepository.findAll(Sort.by("tenDayDu")));

        // 2. Dành cho SINH VIÊN: Chỉ lấy KHOA
        // Sinh viên bắt buộc phải thuộc một Khoa nào đó.
        // (Giả sử bạn dùng Enum LoaiDonVi.KHOA, nếu database lưu String thì sửa thành "KHOA")
        model.addAttribute("listKhoaOnly", donViRepository.findByLoaiDonVi(DonVi.LoaiDonVi.KHOA));

        return "admin/users";
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO, BindingResult result) {
        try {
            if (result.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                result.getFieldErrors().forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );
                return ResponseEntity.badRequest().body(errors);
            }

            UserDTO createdUser = adminUserService.createUser(userDTO);
            log.info("User created successfully: {}", createdUser.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo tài khoản thành công",
                    "user", createdUser
            ));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        try {
            UserDTO user = adminUserService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error getting user {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody UserDTO dto) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

            // --- 1. LOGIC SỬA USERNAME (MỚI THÊM) ---

            // Nếu username gửi lên KHÁC username hiện tại
            if (!user.getUsername().equals(dto.getUsername())) {
                // Kiểm tra xem username mới đã tồn tại trong DB chưa
                if (userRepository.existsByUsername(dto.getUsername())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Username '" + dto.getUsername() + "' đã tồn tại!"));
                }
                // Nếu không trùng thì cho phép cập nhật
                user.setUsername(dto.getUsername());
            }
            // ----------------------------------------

            // Cập nhật các trường khác (Giữ nguyên logic cũ)
            user.setEmail(dto.getEmail());
            user.setSdt(dto.getSdt());

            // Xử lý Role (nếu cho phép sửa)
            if (dto.getRoleId() != null) {
                Role role = roleRepository.findById(dto.getRoleId())
                        .orElseThrow(() -> new RuntimeException("Role không hợp lệ"));
                user.setRole(role);
            }

            // Cập nhật thông tin chi tiết (SinhVien / BTC)
            // ... (Logic cập nhật SinhVien/BTC giữ nguyên như cũ) ...
            if (user.getSinhVien() != null) {
                user.getSinhVien().setMssv(dto.getMssv());
                user.getSinhVien().setTen(dto.getTen());
                // ... các trường khác
            } else if (user.getBtc() != null) {
                user.getBtc().setTen(dto.getTen());
                // ... các trường khác
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        try {
            adminUserService.deleteUser(id);
            log.info("User deleted successfully: {}", id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa tài khoản thành công"
            ));
        } catch (Exception e) {
            log.error("Error deleting user {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@PathVariable Integer id) {
        try {
            adminUserService.resetPassword(id);
            log.info("Password reset successfully for user {}", id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reset mật khẩu thành công. Mật khẩu mới: 123456"
            ));
        } catch (Exception e) {
            log.error("Error resetting password for user {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/roles")
    @ResponseBody
    public ResponseEntity<?> getAllRoles() {
        try {
            List<Role> roles = adminUserService.getAllRoles();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error getting roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/donvi")
    @ResponseBody
    public ResponseEntity<?> getAllDonVi() {
        try {
            List<DonVi> donViList = donViRepository.findAllOrdered();
            return ResponseEntity.ok(donViList);
        } catch (Exception e) {
            log.error("Error getting all don vi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/donvi/by-type/{type}")
    @ResponseBody
    public ResponseEntity<?> getDonViByType(@PathVariable String type) {
        try {
            DonVi.LoaiDonVi tenDonVi = DonVi.LoaiDonVi.valueOf(type.toUpperCase());
            List<DonVi> donViList = donViRepository.findByLoaiDonViOrderByTenDayDu(tenDonVi);
            return ResponseEntity.ok(donViList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Loại đơn vị không hợp lệ: " + type));
        } catch (Exception e) {
            log.error("Error getting don vi by type: {}", type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/donvi/by-role")
    @ResponseBody
    public ResponseEntity<?> getDonViByRole(@RequestParam String roleName) {
        try {
            List<DonVi> donViList;

            if ("SINHVIEN".equalsIgnoreCase(roleName) || "SINH_VIEN".equalsIgnoreCase(roleName)) {
                // Sinh viên chỉ thấy KHOA
                donViList = donViRepository.findKhoaForSinhVien();
                log.debug("Loaded {} KHOA for SINH_VIEN", donViList.size());
            } else if ("BTC".equalsIgnoreCase(roleName)) {
                // BTC thấy CLB, PHONG_BAN, DOAN_HOI (không có KHOA)
                donViList = donViRepository.findDonViForBTC();
                log.debug("Loaded {} don vi for BTC (CLB, PHONG_BAN, DOAN_HOI)", donViList.size());
            } else {
                // Các role khác (ADMIN) thấy tất cả
                donViList = donViRepository.findAllOrdered();
                log.debug("Loaded {} all don vi", donViList.size());
            }

            return ResponseEntity.ok(donViList);
        } catch (Exception e) {
            log.error("Error getting don vi by role: {}", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}