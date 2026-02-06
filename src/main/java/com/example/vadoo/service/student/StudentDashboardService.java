package com.example.vadoo.service.student;

import com.example.vadoo.dto.student.StudentDashboardDTO;
import com.example.vadoo.dto.student.StudentEventDTO;
import com.example.vadoo.dto.student.TicketDTO;
import com.example.vadoo.entity.DangKySuKien;
import com.example.vadoo.entity.DonVi;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.DangKySuKienRepository;
import com.example.vadoo.repository.DonViRepository;
import com.example.vadoo.repository.SuKienRepository;
import com.example.vadoo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardService {

    private final UserRepository userRepository;
    private final SuKienRepository suKienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final DonViRepository donViRepository;

    public StudentDashboardDTO getDashboardData(Integer studentId) {
        // 1. Lấy thông tin sinh viên
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));

        // --- TÍNH ĐIỂM THẬT (LOGIC MỚI: Dùng Java Stream để tính cả điểm phạt) ---

        // Lấy toàn bộ lịch sử (bao gồm cả PRESENT và ABSENT)
        List<DangKySuKien> history = dangKySuKienRepository.findHistoryByStudentId(studentId);

        // 1.1 Tổng điểm tích lũy
        int tongDiem = history.stream()
                .mapToInt(this::calculateRealPoints) // Gọi hàm tính điểm chuẩn
                .sum();

        // Giới hạn không cho âm quá (tùy chọn, nếu muốn min là 0)
        if (tongDiem < 0) tongDiem = 0;

        // 1.2 Điểm cộng trong tháng này
        int currentMonth = LocalDateTime.now().getMonthValue();
        int currentYear = LocalDateTime.now().getYear();

        int diemThangNay = history.stream()
                .filter(d -> {
                    LocalDateTime eventTime = d.getSuKien().getThoigianBatdau();
                    return eventTime.getMonthValue() == currentMonth && eventTime.getYear() == currentYear;
                })
                .mapToInt(this::calculateRealPoints)
                .sum();

        // -----------------------------------------------------------------------

        // 2. Lấy danh sách sự kiện SẮP TỚI (Đã đăng ký) - Giữ nguyên
        // Chú ý: Cần dùng hàm findRegisteredUpcomingEvents (đã khai báo ở Repo) thay vì findRegisteredUpcomingEvents của code cũ nếu tên khác
        List<DangKySuKien> listDangKy = dangKySuKienRepository.findRegisteredUpcomingEvents(
                studentId, LocalDateTime.now());

        List<StudentEventDTO> registeredEvents = listDangKy.stream()
                .map(dk -> mapToDTO(dk.getSuKien(), true))
                .limit(3)
                .collect(Collectors.toList());

        // 3. Lấy danh sách sự kiện GỢI Ý - Giữ nguyên
        List<SuKien> listSuKien = suKienRepository.findHighPointEvents(LocalDateTime.now());

        List<StudentEventDTO> suggestedEvents = listSuKien.stream()
                .filter(sk -> listDangKy.stream().noneMatch(dk -> dk.getSuKien().getId().equals(sk.getId())))
                .map(sk -> mapToDTO(sk, false))
                .limit(2)
                .collect(Collectors.toList());

        // 4. Đóng gói
        return StudentDashboardDTO.builder()
                .hoTen(student.getHoTen()) // Đảm bảo User/SinhVien có hàm getHoTen()
                // .maSinhVien(...) // Nếu DTO cần
                .diemRenLuyen(tongDiem)
                .diemCongThangNay(diemThangNay)
                .soSuKienSapToi(registeredEvents.size())
                .eventsRegistered(registeredEvents)
                .eventsSuggested(suggestedEvents)
                .build();
    }

    /**
     * HÀM QUAN TRỌNG: Tính điểm thực tế (Positive cho PRESENT, Negative cho ABSENT)
     * Logic này cần đồng bộ với trang Lịch sử
     */
    private int calculateRealPoints(DangKySuKien d) {
        int diemGoc = d.getSuKien().getDiemCong();

        if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT) {
            // Có mặt: Lấy điểm đã lưu (hoặc điểm gốc)
            return d.getDiemNhanDuoc() != null ? d.getDiemNhanDuoc() : diemGoc;
        }
        else if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.ABSENT) {
            // Vắng mặt: Trừ 50% làm tròn lên => Ra số âm
            int diemTru = (int) Math.ceil(diemGoc * 0.5);
            return -diemTru;
        }

        return 0; // Các trạng thái khác (nếu có)
    }

    // ... (Các hàm mapToDTO, searchEvents, getStudentTickets, countUpcomingTickets GIỮ NGUYÊN) ...
    // Copy lại các hàm đó vào đây (không cần sửa đổi gì thêm)

    // Helper map Entity -> DTO
    private StudentEventDTO mapToDTO(SuKien sk, boolean isRegistered) {
        int daDangKy = 0;
        try {
            daDangKy = dangKySuKienRepository.countBySuKienAndTrangThai(
                    sk, DangKySuKien.TrangThaiDangKy.REGISTERED);
        } catch (Exception e) { }

        String tenDonVi = (sk.getDonVi() != null) ? sk.getDonVi().getTenDayDu() : "Ban Tổ Chức";

        return StudentEventDTO.builder()
                .id(sk.getId())
                .tieuDe(sk.getTieuDe())
                .posterUrl(sk.getPosterUrl())
                .diaDiem(sk.getDiaDiem())
                .thoigianMoDangky(sk.getThoigianMoDangky())
                .thoigianDongDangky(sk.getThoigianDongDangky())
                .thoigianBatdau(sk.getThoigianBatdau())
                .thoigianKetthuc(sk.getThoigianKetthuc())
                .donViName(tenDonVi)
                .diemCong(sk.getDiemCong())
                .gioiHan(sk.getGioiHan())
                .daDangKy(daDangKy)
                .isRegistered(isRegistered)
                .build();
    }

    public List<TicketDTO> getStudentTickets(Integer studentId) {
        List<DangKySuKien> listDangKy = dangKySuKienRepository.findBySinhVienUserIdOrderByThoigianDangkyDesc(studentId);
        LocalDateTime now = LocalDateTime.now();

        return listDangKy.stream().map(dk -> {
            SuKien sk = dk.getSuKien();
            boolean isPast = sk.getThoigianKetthuc().isBefore(now);

            return TicketDTO.builder()
                    .dangKyId(dk.getId())
                    .eventId(sk.getId())
                    .tenSuKien(sk.getTieuDe())
                    .diaDiem(sk.getDiaDiem())
                    .thoiGianBatDau(sk.getThoigianBatdau())
                    .qrData(dk.getQrRandomString())
                    .trangThaiCheckIn(dk.getTrangthaiThamgia().name())
                    .diemDuocNhan(sk.getDiemCong())
                    .isPast(isPast)
                    .build();
        }).collect(Collectors.toList());
    }

    // 2. BỔ SUNG HÀM NÀY VÀO CUỐI CLASS
    /**
     * Lấy danh sách tên tất cả đơn vị để làm Filter cho trang Sự kiện
     */
    public List<String> getAllDonViNames() {
        return donViRepository.findAll().stream()
                .map(donVi -> donVi.getTenDayDu()) // Lấy tên đầy đủ
                .distinct()                        // Loại bỏ trùng lặp
                .sorted()                          // Sắp xếp A-Z
                .collect(Collectors.toList());
    }

    // ... (Các code cũ giữ nguyên)

    // --- BỔ SUNG CÁC HÀM DƯỚI ĐÂY ---

    /**
     * Tìm kiếm sự kiện cho sinh viên (Có bộ lọc Topic, Keyword, Status)
     */
    public List<StudentEventDTO> searchEvents(Integer studentId, String keyword, String topic, String status) {
        // 1. Lấy danh sách sự kiện từ DB (đã lọc Keyword & Topic bằng SQL)
        List<SuKien> events = suKienRepository.searchStudentEvents(keyword, topic);

        // 2. Lấy danh sách ID các sự kiện SV đã đăng ký để check trạng thái
        List<Integer> registeredEventIds = dangKySuKienRepository.findEventIdsByStudentId(studentId);

        LocalDateTime now = LocalDateTime.now();

        // 3. Map sang DTO và Lọc theo Status (Registered/Open/Ended) bằng Java
        return events.stream()
                .map(sk -> {
                    boolean isRegistered = registeredEventIds.contains(sk.getId());
                    return mapToDTO(sk, isRegistered); // Gọi lại hàm mapToDTO đã có trong class này
                })
                .filter(dto -> filterByStatus(dto, status, now))
                .collect(Collectors.toList());
    }

    /**
     * Hàm phụ trợ: Lọc theo trạng thái (Đã đăng ký, Đang mở, Đã kết thúc)
     */
    private boolean filterByStatus(StudentEventDTO dto, String status, LocalDateTime now) {
        if (status == null || status.equals("all") || status.isEmpty()) {
            return true;
        }

        switch (status) {
            case "registered":
                // Lấy sự kiện đã đăng ký
                return dto.isRegistered();

            case "open":
                // ĐANG MỞ = Chưa đăng ký + Trong thời gian đăng ký + Còn chỗ
                boolean isNotRegistered = !dto.isRegistered();
                boolean isRegistrationTime = now.isAfter(dto.getThoigianMoDangky())
                        && now.isBefore(dto.getThoigianDongDangky());
                boolean hasSlot = (dto.getGioiHan() == 0) || (dto.getDaDangKy() < dto.getGioiHan());

                return isNotRegistered && isRegistrationTime && hasSlot;

            case "ended":
                // Đã kết thúc
                return dto.getThoigianKetthuc().isBefore(now);

            default:
                return true;
        }
    }

    /**
     * Hàm tiện ích: Đếm số vé sắp diễn ra để hiện badge thông báo lên Sidebar
     */
    public int countUpcomingTickets(Integer studentId) {
        // Lấy danh sách đăng ký của sinh viên (đã sắp xếp mới nhất)
        List<DangKySuKien> listDangKy = dangKySuKienRepository.findBySinhVienUserIdOrderByThoigianDangkyDesc(studentId);

        LocalDateTime now = LocalDateTime.now();

        // Đếm số lượng vé mà sự kiện chưa kết thúc (thời gian kết thúc > hiện tại)
        return (int) listDangKy.stream()
                .filter(dk -> dk.getSuKien().getThoigianKetthuc().isAfter(now))
                .count();
    }
}