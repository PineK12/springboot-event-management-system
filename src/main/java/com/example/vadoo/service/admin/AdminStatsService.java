package com.example.vadoo.service.admin;

import com.example.vadoo.aop.LogActivity;
import com.example.vadoo.dto.admin.*;
import com.example.vadoo.entity.DangKySuKien;
import com.example.vadoo.entity.DonVi;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.entity.SuKien.TrangThaiSuKien;
import com.example.vadoo.repository.DangKySuKienRepository;
import com.example.vadoo.repository.EventFeedbackRepository;
import com.example.vadoo.repository.SuKienRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatsService {

    private final SuKienRepository suKienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final EventFeedbackRepository eventFeedbackRepository;

    // ========== THỐNG KÊ TỔNG QUAN ==========

    public StatsDTO getOverallStats() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now().withDayOfMonth(
                YearMonth.now().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);

        long totalEvents = suKienRepository.countValidEventsBetweenDates(startOfMonth, endOfMonth);
        long totalRegistrations = dangKySuKienRepository.countAllRegistrations();
        long totalAttendance = dangKySuKienRepository.countAttendedRegistrations();

        int attendanceRate = totalRegistrations > 0
                ? (int) Math.round((double) totalAttendance / totalRegistrations * 100)
                : 0;

        // --- SỬA LẠI LOGIC TÍNH ĐIỂM ---
        Double rawAvg = eventFeedbackRepository.getGlobalAverageRating();

        // Làm tròn 1 chữ số thập phân (VD: 4.5)
        Double avgRating = Math.round(rawAvg * 10.0) / 10.0;

        return StatsDTO.builder()
                .totalEvents(totalEvents)
                .totalRegistrations(totalRegistrations)
                .totalAttendance(totalAttendance)
                .attendanceRate(attendanceRate)
                .averageRating(avgRating)
                .monthlyStats(getMonthlyStats())
                .categoryStats(getCategoryStats())
                .build();
    }

    // ========== BIỂU ĐỒ THEO THÁNG ==========

    public MonthlyStatsDTO getMonthlyStats() {
        List<String> labels = Arrays.asList("T1", "T2", "T3", "T4", "T5", "T6",
                "T7", "T8", "T9", "T10", "T11", "T12");
        List<Integer> data = new ArrayList<>();

        int currentYear = LocalDateTime.now().getYear();

        for (int month = 1; month <= 12; month++) {
            LocalDateTime startOfMonth = LocalDateTime.of(currentYear, month, 1, 0, 0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

            // CHANGED: Count valid EVENTS instead of REGISTRATIONS
            int count = (int) suKienRepository.countValidEventsBetweenDates(startOfMonth, endOfMonth);
            data.add(count);
        }

        return MonthlyStatsDTO.builder()
                .labels(labels)
                .data(data)
                .build();
    }

    // ========== BIỂU ĐỒ PHÂN LOẠI - ✅ FIXED: Dùng ENUM ==========

    public CategoryStatsDTO getCategoryStats() {
        List<SuKien> validEvents = suKienRepository.findAllValidEvents();

        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        categoryCount.put("Khoa", 0);
        categoryCount.put("Câu lạc bộ", 0);
        categoryCount.put("Đoàn - Hội", 0);
        categoryCount.put("Phòng ban", 0);
        categoryCount.put("Khác", 0);

        for (SuKien sk : validEvents) {
            String category = determineCategoryFromEvent(sk);
            categoryCount.put(category, categoryCount.get(category) + 1);
        }

        return CategoryStatsDTO.builder()
                .labels(new ArrayList<>(categoryCount.keySet()))
                .data(new ArrayList<>(categoryCount.values()))
                .build();
    }

    // ✅ FIXED: Dùng ENUM thay vì string matching
    private String determineCategoryFromEvent(SuKien suKien) {
        if (suKien.getDonVi() == null) return "Khác";

        // ✅ SỬ DỤNG ENUM tenDonVi
        DonVi.LoaiDonVi tenDonVi = suKien.getDonVi().getLoaiDonVi();

        if (tenDonVi == null) return "Khác";

        switch (tenDonVi) {
            case KHOA:
                return "Khoa";
            case CLB:
                return "Câu lạc bộ";
            case DOAN_HOI:
                return "Đoàn - Hội";
            case PHONG_BAN:
                return "Phòng ban";
            default:
                return "Khác";
        }
    }

    // ========== DANH SÁCH SỰ KIỆN ==========

    public List<EventStatsDTO> getAllEventStats() {
        List<SuKien> events = suKienRepository.findAllWithRelations();
        return events.stream()
                .filter(e -> e.getTrangThai() == TrangThaiSuKien.APPROVED || e.getTrangThai() == TrangThaiSuKien.COMPLETED)
                .map(this::convertToEventStatsDTO)
                .collect(Collectors.toList());
    }

    public List<EventStatsDTO> getEventStatsByFilter(String filter) {
        List<SuKien> events = suKienRepository.findAllWithRelations();
        LocalDateTime now = LocalDateTime.now();

        return events.stream()
                .filter(e -> e.getTrangThai() != TrangThaiSuKien.CANCELLED && e.getTrangThai() != TrangThaiSuKien.REJECTED)
                .filter(event -> {
                    switch (filter) {
                        case "upcoming":
                            return event.getThoigianBatdau().isAfter(now);
                        case "ongoing":
                            return event.getThoigianBatdau().isBefore(now)
                                    && event.getThoigianKetthuc().isAfter(now);
                        case "completed":
                            return event.getThoigianKetthuc().isBefore(now);
                        default:
                            return true;
                    }
                })
                .map(this::convertToEventStatsDTO)
                .collect(Collectors.toList());
    }

    private EventStatsDTO convertToEventStatsDTO(SuKien suKien) {
        long soLuongDangKy = dangKySuKienRepository.countBySuKien(suKien);
        long soLuongDiemDanh = dangKySuKienRepository.countAttendedBySuKien(suKien);

        int tyLeDiemDanh = soLuongDangKy > 0
                ? (int) Math.round((double) soLuongDiemDanh / soLuongDangKy * 100)
                : 0;

        String status = determineEventStatus(suKien);

        return EventStatsDTO.builder()
                .id(suKien.getId())
                .tenSuKien(suKien.getTieuDe())
                .category(determineCategoryFromEvent(suKien))
                .thoiGianBatDau(suKien.getThoigianBatdau())
                .thoiGianKetThuc(suKien.getThoigianKetthuc())
                .soLuongDangKy((int) soLuongDangKy)
                .soLuongDiemDanh((int) soLuongDiemDanh)
                .gioiHan(suKien.getGioiHan())
                .tyLeDiemDanh(tyLeDiemDanh)
                .status(status)
                .trangThai(suKien.getTrangThai())
                .donViName(suKien.getDonVi() != null ? suKien.getDonVi().getTenDayDu() : "N/A")
                .build();
    }

    private String determineEventStatus(SuKien suKien) {
        if (suKien.getTrangThai() == TrangThaiSuKien.CANCELLED) return "cancelled";
        if (suKien.getTrangThai() == TrangThaiSuKien.REJECTED) return "rejected";
        if (suKien.getTrangThai() == TrangThaiSuKien.PENDING) return "pending";

        LocalDateTime now = LocalDateTime.now();

        if (suKien.getThoigianBatdau().isAfter(now)) {
            return "upcoming";
        } else if (suKien.getThoigianKetthuc().isBefore(now)) {
            return "completed";
        } else {
            return "ongoing";
        }
    }

    public EventStatsDTO getEventById(Integer id) {
        SuKien suKien = suKienRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        return convertToEventStatsDTO(suKien);
    }

    // ========== DANH SÁCH SINH VIÊN ==========

    public List<StudentAttendanceDTO> getStudentsBySuKien(Integer suKienId) {
        List<DangKySuKien> registrations = dangKySuKienRepository.findBySuKienIdWithSinhVien(suKienId);

        return registrations.stream()
                .map(this::convertToStudentAttendanceDTO)
                .collect(Collectors.toList());
    }

    public List<StudentAttendanceDTO> getStudentsBySuKienAndFilter(Integer suKienId, String filter) {
        List<DangKySuKien> registrations = dangKySuKienRepository.findBySuKienIdWithSinhVien(suKienId);

        return registrations.stream()
                .filter(reg -> {
                    switch (filter) {
                        case "attended":
                            return reg.getThoigianCheckin() != null;
                        case "not-attended":
                            return reg.getThoigianCheckin() == null;
                        default:
                            return true;
                    }
                })
                .map(this::convertToStudentAttendanceDTO)
                .collect(Collectors.toList());
    }

    private StudentAttendanceDTO convertToStudentAttendanceDTO(DangKySuKien dangKy) {
        return StudentAttendanceDTO.builder()
                .dangKyId(dangKy.getId())
                .mssv(dangKy.getSinhVien().getMssv())
                .ten(dangKy.getSinhVien().getTen())
                .tenLop(dangKy.getSinhVien().getTenLop())
                .email(dangKy.getSinhVien().getUser().getEmail())
                .sdt(dangKy.getSinhVien().getUser().getSdt())
                .daDiemDanh(dangKy.getThoigianCheckin() != null)
                .thoiGianDiemDanh(dangKy.getThoigianCheckin())
                .thoiGianDangKy(dangKy.getThoigianDangky())
                .ghiChu(dangKy.getGhiChu())
                .build();
    }

    // ========== TOGGLE ĐIỂM DANH ==========

    @Transactional
    @LogActivity(action = "TOGGLE_ATTENDANCE", description = "Điều chỉnh điểm danh thủ công", targetTable = "dangky_sukien")
    public void toggleAttendance(Long dangKyId) {
        DangKySuKien dangKy = dangKySuKienRepository.findById(dangKyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký"));

        if (dangKy.getThoigianCheckin() == null) {
            dangKy.setThoigianCheckin(LocalDateTime.now());
            dangKy.setTrangthaiThamgia(DangKySuKien.TrangThaiThamGia.PRESENT);
        } else {
            dangKy.setThoigianCheckin(null);
            dangKy.setTrangthaiThamgia(DangKySuKien.TrangThaiThamGia.ABSENT);
        }

        dangKySuKienRepository.save(dangKy);
    }
}