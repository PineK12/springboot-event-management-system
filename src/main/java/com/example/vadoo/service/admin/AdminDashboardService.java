package com.example.vadoo.service.admin;

import com.example.vadoo.dto.admin.DashboardStatsDTO;
import com.example.vadoo.dto.admin.PendingEventDTO;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.repository.DangKySuKienRepository;
import com.example.vadoo.repository.SuKienRepository;
import com.example.vadoo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý Dashboard cho ADMIN
 * Cung cấp thống kê tổng quan hệ thống
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final SuKienRepository suKienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Lấy thống kê tổng quan cho dashboard
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        log.info("Fetching admin dashboard statistics");

        long totalUsers = userRepository.count();
        long activeEvents = suKienRepository.countActiveEvents();
        long pendingEvents = suKienRepository.countByTrangThai(SuKien.TrangThaiSuKien.PENDING);
        long totalPoints = dangKySuKienRepository.calculateTotalPoints();

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeEvents(activeEvents)
                .pendingEvents(pendingEvents)
                .totalPoints(totalPoints)
                .build();
    }

    /**
     * Lấy danh sách sự kiện chờ duyệt gần đây (tối đa 10 events)
     */
    @Transactional(readOnly = true)
    public List<PendingEventDTO> getRecentPendingEvents() {
        log.info("Fetching recent pending events for admin");

        List<SuKien> pendingEvents = suKienRepository.findTopPendingEvents();

        return pendingEvents.stream()
                .limit(10)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thống kê theo đơn vị
     */
    @Transactional(readOnly = true)
    public List<EventStatsByDonViDTO> getEventStatsByDonVi() {
        // TODO: Implement nếu cần
        return List.of();
    }

    /**
     * Lấy xu hướng đăng ký theo thời gian
     */
    @Transactional(readOnly = true)
    public List<RegistrationTrendDTO> getRegistrationTrend(int days) {
        // TODO: Implement nếu cần
        return List.of();
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert entity sang DTO
     */
    private PendingEventDTO convertToDTO(SuKien suKien) {
        String btcName = "N/A";
        String donViName = "N/A";

        if (suKien.getBtc() != null) {
            if (suKien.getBtc().getBtc() != null) {
                btcName = suKien.getBtc().getBtc().getTen();
            } else if (suKien.getBtc().getSinhVien() != null) {
                btcName = suKien.getBtc().getSinhVien().getTen();
            } else {
                btcName = suKien.getBtc().getUsername();
            }
        }

        if (suKien.getDonVi() != null) {
            donViName = suKien.getDonVi().getTenDayDu();
        }

        return PendingEventDTO.builder()
                .id(suKien.getId())
                .tieuDe(suKien.getTieuDe())
                .btcName(btcName)
                .donViName(donViName)
                .ngayTao(suKien.getThoigianTao().format(DATE_FORMATTER))
                .trangThai(getTrangThaiDisplay(suKien.getTrangThai()))
                .trangThaiClass(getTrangThaiClass(suKien.getTrangThai()))
                .build();
    }

    /**
     * Lấy text hiển thị của trạng thái
     */
    private String getTrangThaiDisplay(SuKien.TrangThaiSuKien trangThai) {
        return switch (trangThai) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
            case DRAFT -> "Nháp";
            case CANCELLED -> "Đã hủy";
            case COMPLETED -> "Hoàn thành";
        };
    }

    /**
     * Lấy CSS class cho trạng thái
     */
    private String getTrangThaiClass(SuKien.TrangThaiSuKien trangThai) {
        return switch (trangThai) {
            case PENDING -> "bg-yellow-100 text-yellow-700";
            case APPROVED -> "bg-green-100 text-green-700";
            case REJECTED -> "bg-red-100 text-red-700";
            case DRAFT -> "bg-gray-100 text-gray-700";
            case CANCELLED -> "bg-orange-100 text-orange-700";
            case COMPLETED -> "bg-blue-100 text-blue-700";
        };
    }

    // Inner DTOs (nếu cần)
    @lombok.Data
    @lombok.Builder
    public static class EventStatsByDonViDTO {
        private String donViName;
        private long totalEvents;
        private long approvedEvents;
        private long pendingEvents;
    }

    @lombok.Data
    @lombok.Builder
    public static class RegistrationTrendDTO {
        private String date;
        private long registrations;
    }
}