package com.example.vadoo.service.admin;

import com.example.vadoo.aop.LogActivity;
import com.example.vadoo.dto.admin.UserDTO;
import com.example.vadoo.entity.*;
import com.example.vadoo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final SinhVienRepository sinhVienRepository;
    private final BtcRepository btcRepository;
    private final DonViRepository donViRepository;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Helper check Role Name
    private boolean isSinhVien(String roleName) {
        return "SINHVIEN".equalsIgnoreCase(roleName) || "SINH_VIEN".equalsIgnoreCase(roleName);
    }

    private boolean isBTC(String roleName) {
        return "BTC".equalsIgnoreCase(roleName);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        List<UserDTO> dtos = users.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, users.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsers(String keyword, String roleFilter, Pageable pageable) {
        Page<User> users;
        String adminRoleName = "ADMIN";

        if (keyword != null && !keyword.trim().isEmpty()) {
            if (roleFilter != null && !roleFilter.equals("all")) {
                Role role = roleRepository.findByTenRole(roleFilter)
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
                users = userRepository.findByUsernameContainingOrEmailContainingAndRole(
                        keyword, keyword, role, pageable);
            } else {
                users = userRepository.searchByKeywordAndExcludeRole(keyword, adminRoleName, pageable);
            }
        } else {
            if (roleFilter != null && !roleFilter.equals("all")) {
                Role role = roleRepository.findByTenRole(roleFilter)
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
                users = userRepository.findByRole(role, pageable);
            } else {
                users = userRepository.findByRole_TenRoleNot(adminRoleName, pageable);
            }
        }

        List<UserDTO> dtos = users.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, users.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return convertToDTO(user);
    }

    @Transactional
    @LogActivity(action = "CREATE_USER", description = "Tạo tài khoản mới", targetTable = "users")
    public UserDTO createUser(UserDTO dto) {
        // 1. Basic validations
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // 2. Password validation
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }
        if (dto.getPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        // 3. Role validation
        if (dto.getRoleId() == null) {
            throw new RuntimeException("Vui lòng chọn vai trò");
        }

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        if ("ADMIN".equalsIgnoreCase(role.getTenRole()) || "SUPER_ADMIN".equalsIgnoreCase(role.getTenRole())) {
            throw new RuntimeException("Không thể tạo tài khoản ADMIN qua chức năng này");
        }

        // 4. Role-specific validation
        dto.setRoleName(role.getTenRole());
        try {
            dto.validateForRole();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        }

        // 5. Additional validations for SinhVien
        if (isSinhVien(role.getTenRole())) {
            if (sinhVienRepository.existsByMssv(dto.getMssv())) {
                throw new RuntimeException("MSSV đã tồn tại trong hệ thống");
            }
        }

        // 6. Create User entity
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .sdt(dto.getSdt())
                .role(role)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // 7. Create role-specific profile
        if (isSinhVien(role.getTenRole())) {
            createSinhVienProfile(user, dto);
        } else if (isBTC(role.getTenRole())) {
            createBtcProfile(user, dto);
        }

        log.info("Created user: {}, role: {}", user.getUsername(), role.getTenRole());
        return convertToDTO(user);
    }

    @Transactional
    @LogActivity(action = "UPDATE_USER", description = "Cập nhật thông tin tài khoản", targetTable = "users")
    public UserDTO updateUser(Integer id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Check email uniqueness exclude current user
        if (!user.getEmail().equals(dto.getEmail()) &&
                userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        user.setEmail(dto.getEmail());
        user.setSdt(dto.getSdt());
        // Không cho phép update isActive tại đây nếu không cần thiết
        // user.setIsActive(dto.getIsActive());

        String roleName = user.getRole().getTenRole();

        // ✅ SỬA LỖI: Dùng hàm helper để check role chính xác
        if (isSinhVien(roleName)) {
            updateSinhVienProfile(user, dto);
        } else if (isBTC(roleName)) {
            updateBtcProfile(user, dto);
        }

        user = userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());
        return convertToDTO(user);
    }

    @Transactional
    @LogActivity(action = "DELETE_USER", description = "Xóa tài khoản người dùng", targetTable = "users")
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Nếu DB chưa set Cascade DELETE, cần xóa thủ công profile trước
        if (user.getSinhVien() != null) {
            sinhVienRepository.delete(user.getSinhVien());
        }
        if (user.getBtc() != null) {
            btcRepository.delete(user.getBtc());
        }

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    @Transactional
    @LogActivity(action = "RESET_PASSWORD", description = "Reset mật khẩu về mặc định", targetTable = "users")
    public void resetPassword(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setPassword(passwordEncoder.encode("123456"));
        userRepository.save(user);
        log.info("Reset password for user: {}", user.getUsername());
    }

    private void createSinhVienProfile(User user, UserDTO dto) {
        if (dto.getDonViId() == null) {
            throw new RuntimeException("Vui lòng chọn Khoa/Đơn vị cho sinh viên");
        }
        DonVi donVi = donViRepository.findById(dto.getDonViId())
                .orElseThrow(() -> new RuntimeException("Đơn vị không tồn tại"));

        SinhVien sinhVien = SinhVien.builder()
                .user(user)
                .mssv(dto.getMssv())
                .ten(dto.getTen())
                .ngaySinh(dto.getNgaySinh())
                .gioiTinh(SinhVien.GioiTinh.valueOf(dto.getGioiTinh()))
                .tenLop(dto.getTenLop())
                .donVi(donVi)
                .build();

        sinhVienRepository.save(sinhVien);
    }

    private void createBtcProfile(User user, UserDTO dto) {
        if (dto.getDonViId() == null) {
            throw new RuntimeException("Vui lòng chọn Đơn vị cho BTC");
        }
        DonVi donVi = donViRepository.findById(dto.getDonViId())
                .orElseThrow(() -> new RuntimeException("Đơn vị không tồn tại"));

        Btc btc = Btc.builder()
                .user(user)
                .ten(dto.getTen())
                .donVi(donVi)
                .chucVu(dto.getChucVu())
                .build();

        btcRepository.save(btc);
    }

    private void updateSinhVienProfile(User user, UserDTO dto) {
        SinhVien sinhVien = user.getSinhVien();
        if (sinhVien == null) {
            // Trường hợp user có role SV nhưng chưa có data trong bảng sinh_vien -> Tạo mới
            createSinhVienProfile(user, dto);
            return;
        }

        sinhVien.setTen(dto.getTen());
        if (dto.getNgaySinh() != null) sinhVien.setNgaySinh(dto.getNgaySinh());
        if (dto.getGioiTinh() != null) sinhVien.setGioiTinh(SinhVien.GioiTinh.valueOf(dto.getGioiTinh()));
        sinhVien.setTenLop(dto.getTenLop());
        sinhVien.setMssv(dto.getMssv()); // Cho phép sửa MSSV nếu cần

        if (dto.getDonViId() != null) {
            DonVi donVi = donViRepository.findById(dto.getDonViId())
                    .orElseThrow(() -> new RuntimeException("Đơn vị không tồn tại"));
            sinhVien.setDonVi(donVi);
        }

        sinhVienRepository.save(sinhVien);
    }

    private void updateBtcProfile(User user, UserDTO dto) {
        Btc btc = user.getBtc();
        if (btc == null) {
            // Trường hợp user có role BTC nhưng chưa có data -> Tạo mới
            createBtcProfile(user, dto);
            return;
        }

        btc.setTen(dto.getTen());
        btc.setChucVu(dto.getChucVu());

        if (dto.getDonViId() != null) {
            DonVi donVi = donViRepository.findById(dto.getDonViId())
                    .orElseThrow(() -> new RuntimeException("Đơn vị không tồn tại"));
            btc.setDonVi(donVi);
        }

        btcRepository.save(btc);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .sdt(user.getSdt())
                .roleId(user.getRole().getId())
                .roleName(user.getRole().getTenRole())
                .isActive(user.getIsActive())
                .avatarUrl(user.getAvatarUrl())
                .thoiGianTao(user.getThoigianTao() != null ?
                        user.getThoigianTao().format(DATE_FORMATTER) : "")
                .thoiGianCapNhat(user.getThoigianCapnhat() != null ?
                        user.getThoigianCapnhat().format(DATE_FORMATTER) : "")
                .build();

        if (user.getSinhVien() != null) {
            SinhVien sv = user.getSinhVien();
            dto.setMssv(sv.getMssv());
            dto.setTen(sv.getTen());
            dto.setNgaySinh(sv.getNgaySinh());
            dto.setGioiTinh(sv.getGioiTinh() != null ? sv.getGioiTinh().name() : "NAM");
            dto.setTenLop(sv.getTenLop());
            if (sv.getDonVi() != null) {
                dto.setDonViId(sv.getDonVi().getId());
                dto.setDonViName(sv.getDonVi().getTenDayDu());
            }
        } else if (user.getBtc() != null) {
            Btc btc = user.getBtc();
            dto.setTen(btc.getTen());
            dto.setChucVu(btc.getChucVu());
            if (btc.getDonVi() != null) {
                dto.setDonViId(btc.getDonVi().getId());
                dto.setDonViName(btc.getDonVi().getTenDayDu());
            }
        }

        return dto;
    }

    // Các hàm read-only khác giữ nguyên...
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() { return roleRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() { return permissionRepository.findAll(); }

    @Transactional(readOnly = true)
    public Set<Permission> getUserPermissions(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return user.getRole().getPermissions();
    }

    @Transactional(readOnly = true)
    public long getTotalUsers() { return userRepository.count(); }

    @Transactional(readOnly = true)
    public long getActiveUsers() { return userRepository.countByIsActive(true); }

    @Transactional(readOnly = true)
    public long getTotalStudents() { return userRepository.countByRole_TenRole("SINH_VIEN"); }
}