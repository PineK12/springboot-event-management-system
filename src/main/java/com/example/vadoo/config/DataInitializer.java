package com.example.vadoo.config;

import com.example.vadoo.entity.*;
import com.example.vadoo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final DonViRepository donViRepository;
    private final SinhVienRepository sinhVienRepository;
    private final BtcRepository btcRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only initialize if no roles exist
        if (roleRepository.count() == 0) {
            log.info("Initializing sample data...");
            initializeData();
            log.info("Sample data initialized successfully!");
        } else {
            log.info("Data already exists, skipping initialization.");
        }
    }

    private void initializeData() {
        // Create Permissions
        Permission pManageUsers = createPermission("MANAGE_USERS", "Quản lý người dùng");
        Permission pManageEvents = createPermission("MANAGE_EVENTS", "Quản lý sự kiện");
        Permission pApproveEvents = createPermission("APPROVE_EVENTS", "Duyệt sự kiện");
        Permission pCreateEvents = createPermission("CREATE_EVENTS", "Tạo sự kiện");
        Permission pCheckin = createPermission("CHECKIN", "Check-in sự kiện");
        Permission pRegisterEvents = createPermission("REGISTER_EVENTS", "Đăng ký sự kiện");
        Permission pViewEvents = createPermission("VIEW_EVENTS", "Xem sự kiện");
        Permission pViewReports = createPermission("VIEW_REPORTS", "Xem báo cáo");
        Permission pManageSystem = createPermission("MANAGE_SYSTEM", "Quản lý hệ thống");

        // Create Roles
        Role adminRole = createRole("ADMIN", "Quản trị viên hệ thống");
        adminRole.getPermissions().add(pManageUsers);
        adminRole.getPermissions().add(pManageEvents);
        adminRole.getPermissions().add(pApproveEvents);
        adminRole.getPermissions().add(pViewReports);
        adminRole.getPermissions().add(pManageSystem);
        roleRepository.save(adminRole);

        Role btcRole = createRole("BTC", "Ban tổ chức sự kiện");
        btcRole.getPermissions().add(pCreateEvents);
        btcRole.getPermissions().add(pCheckin);
        btcRole.getPermissions().add(pViewReports);
        roleRepository.save(btcRole);

        Role sinhVienRole = createRole("SINHVIEN", "Sinh viên");
        sinhVienRole.getPermissions().add(pRegisterEvents);
        sinhVienRole.getPermissions().add(pViewEvents);
        roleRepository.save(sinhVienRole);

        // Create DonVi
        DonVi khoaCNTT = createDonVi(DonVi.LoaiDonVi.KHOA, "Khoa Công nghệ Thông tin", null);
        DonVi khoaKT = createDonVi(DonVi.LoaiDonVi.KHOA, "Khoa Kinh tế", null);
        DonVi doanTN = createDonVi(DonVi.LoaiDonVi.DOAN_HOI, "Đoàn Thanh niên", null);
        DonVi clbIT = createDonVi(DonVi.LoaiDonVi.CLB, "CLB Tin học", khoaCNTT);

        // Create Users
        // Admin user
        User adminUser = createUser("admin", "admin123", "admin@vadoo.edu.vn", "0901234567", adminRole);

        // BTC user
        User btcUser = createUser("btc01", "btc123", "btc01@vadoo.edu.vn", "0902345678", btcRole);
        Btc btc = Btc.builder()
                .user(btcUser)
                .ten("Nguyễn Văn BTC")
                .donVi(doanTN)
                .chucVu("Trưởng ban tổ chức")
                .build();
        btcRepository.save(btc);

        // SinhVien user
        User svUser = createUser("sv001", "sv123", "sv001@vadoo.edu.vn", "0903456789", sinhVienRole);
        SinhVien sv = SinhVien.builder()
                .user(svUser)
                .mssv("20110001")
                .ten("Trần Văn Sinh Viên")
                .gioiTinh(SinhVien.GioiTinh.NAM)
                .tenLop("CNTT01")
                .donVi(khoaCNTT)
                .build();
        sinhVienRepository.save(sv);

        log.info("Created users: admin/admin123, btc01/btc123, sv001/sv123");
    }

    private Permission createPermission(String code, String moTa) {
        Permission permission = Permission.builder()
                .code(code)
                .moTa(moTa)
                .build();
        return permissionRepository.save(permission);
    }

    private Role createRole(String tenRole, String moTa) {
        Role role = Role.builder()
                .tenRole(tenRole)
                .moTa(moTa)
                .build();
        return roleRepository.save(role);
    }

    private DonVi createDonVi(DonVi.LoaiDonVi loai, String tenDayDu, DonVi parent) {
        DonVi donVi = DonVi.builder()
                .loaiDonVi(loai)
                .tenDayDu(tenDayDu)
                .parent(parent)
                .build();
        return donViRepository.save(donVi);
    }

    private User createUser(String username, String password, String email, String sdt, Role role) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .sdt(sdt)
                .role(role)
                .isActive(true)
                .build();
        return userRepository.save(user);
    }
}