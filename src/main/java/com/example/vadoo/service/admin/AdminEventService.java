package com.example.vadoo.service.admin;

import com.example.vadoo.aop.LogActivity;
import com.example.vadoo.dto.admin.EventDTO;
import com.example.vadoo.dto.admin.EventRequestDTO;
import com.example.vadoo.entity.Btc;
import com.example.vadoo.entity.DonVi;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.entity.SuKien.TrangThaiSuKien;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý sự kiện cho ADMIN
 * Admin có quyền: tạo, sửa, duyệt, từ chối, hủy, xóa TẤT CẢ sự kiện
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminEventService {

    private final SuKienRepository suKienRepository;
    private final DonViRepository donViRepository;
    private final UserRepository userRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final BtcRepository btcRepository;

    /**
     * Lấy tất cả sự kiện (Admin view)
     */
    public List<EventDTO> getAllEvents() {
        return suKienRepository.findAllWithRelations().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm và filter sự kiện
     */
    public List<EventDTO> searchEvents(String keyword, String statusFilter) {
        List<SuKien> allEvents = suKienRepository.findAllWithRelations();
        LocalDateTime now = LocalDateTime.now();

        return allEvents.stream()
                .filter(e -> keyword == null || keyword.isEmpty() ||
                        e.getTieuDe().toLowerCase().contains(keyword.toLowerCase()) ||
                        (e.getDonVi() != null && e.getDonVi().getTenDayDu().toLowerCase().contains(keyword.toLowerCase())))
                .filter(e -> {
                    if (statusFilter == null || statusFilter.equals("all")) return true;
                    TrangThaiSuKien dbStatus = e.getTrangThai();
                    if (dbStatus == TrangThaiSuKien.APPROVED) {
                        if (statusFilter.equals("UPCOMING")) return now.isBefore(e.getThoigianBatdau());
                        if (statusFilter.equals("HAPPENING")) return (now.isAfter(e.getThoigianBatdau()) || now.isEqual(e.getThoigianBatdau())) &&
                                (now.isBefore(e.getThoigianKetthuc()) || now.isEqual(e.getThoigianKetthuc()));
                        if (statusFilter.equals("ENDED")) return now.isAfter(e.getThoigianKetthuc());
                        if (statusFilter.equals("APPROVED")) return true;
                    }
                    return dbStatus.name().equals(statusFilter);
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết sự kiện
     */
    public EventDTO getEventById(Integer id) {
        SuKien suKien = suKienRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        return convertToDTO(suKien);
    }

    /**
     * Tạo sự kiện mới (Admin tạo cho BTC)
     */
    @Transactional
    @LogActivity(action = "ADMIN_CREATE_EVENT", description = "Admin tạo sự kiện mới", targetTable = "su_kien")
    public EventDTO createEvent(EventRequestDTO request, String adminUsername) {
        User adminUser = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Admin thực hiện thao tác"));

        if (request.getBtcId() == null) {
            throw new RuntimeException("Vui lòng chọn cán bộ phụ trách (BTC)");
        }

        User btcUser = userRepository.findById(request.getBtcId())
                .orElseThrow(() -> new RuntimeException("Tài khoản BTC không tồn tại"));

        Btc btcInfo = btcRepository.findById(btcUser.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản '" + btcUser.getUsername() + "' chưa được cấu hình thông tin BTC/Đơn vị"));

        DonVi donViCuaBtc = btcInfo.getDonVi();

        // Xử lý thời gian mặc định nếu null
        LocalDateTime moDangKy = request.getThoiGianMoDangKy() != null ?
                request.getThoiGianMoDangKy() : LocalDateTime.now();

        LocalDateTime dongDangKy = request.getThoiGianDongDangKy() != null ?
                request.getThoiGianDongDangKy() : request.getThoiGianBatDau().minusHours(1);

        // Validate thời gian
        validateEventDates(request.getThoiGianBatDau(), request.getThoiGianKetThuc(), moDangKy, dongDangKy);

        SuKien suKien = SuKien.builder()
                .tieuDe(request.getTenSuKien())
                .moTa(request.getMoTa())
                .noiDung(request.getNoiDung())
                .posterUrl(request.getPosterUrl())
                .thoigianBatdau(request.getThoiGianBatDau())
                .thoigianKetthuc(request.getThoiGianKetThuc())
                .thoigianMoDangky(moDangKy)
                .thoigianDongDangky(dongDangKy)
                .diaDiem(request.getDiaDiem())
                .gioiHan(request.getGioiHanDangKy() != null ? request.getGioiHanDangKy() : 0)
                .diemCong(request.getDiemRenLuyen() != null ? request.getDiemRenLuyen() : 0)
                .diemThoiThieu(request.getDiemThoiThieu() != null ? request.getDiemThoiThieu() : 0)
                .btc(btcUser)
                .donVi(donViCuaBtc)
                .admin(adminUser)
                .trangThai(TrangThaiSuKien.APPROVED) // Admin tạo -> tự động duyệt
                .build();

        SuKien saved = suKienRepository.save(suKien);
        log.info("Admin {} created event: {}", adminUsername, saved.getTieuDe());

        return convertToDTO(saved);
    }

    /**
     * Cập nhật sự kiện (Admin có thể sửa mọi sự kiện)
     */
    @Transactional
    @LogActivity(action = "ADMIN_UPDATE_EVENT", description = "Admin cập nhật sự kiện", targetTable = "su_kien")
    public EventDTO updateEvent(Integer id, EventRequestDTO request) {
        SuKien suKien = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        LocalDateTime start = request.getThoiGianBatDau();
        LocalDateTime end = request.getThoiGianKetThuc();
        LocalDateTime openReg = request.getThoiGianMoDangKy() != null ? request.getThoiGianMoDangKy() : suKien.getThoigianMoDangky();
        LocalDateTime closeReg = request.getThoiGianDongDangKy() != null ? request.getThoiGianDongDangKy() : suKien.getThoigianDongDangky();

        validateEventDates(start, end, openReg, closeReg);

        suKien.setTieuDe(request.getTenSuKien());
        suKien.setMoTa(request.getMoTa());
        suKien.setNoiDung(request.getNoiDung());
        suKien.setPosterUrl(request.getPosterUrl());
        suKien.setThoigianBatdau(start);
        suKien.setThoigianKetthuc(end);
        suKien.setThoigianMoDangky(openReg);
        suKien.setThoigianDongDangky(closeReg);
        suKien.setDiaDiem(request.getDiaDiem());
        suKien.setGioiHan(request.getGioiHanDangKy() != null ? request.getGioiHanDangKy() : 0);
        suKien.setDiemCong(request.getDiemRenLuyen() != null ? request.getDiemRenLuyen() : 0);
        suKien.setDiemThoiThieu(request.getDiemThoiThieu() != null ? request.getDiemThoiThieu() : 0);

        if (request.getBtcId() != null && !request.getBtcId().equals(suKien.getBtc().getId())) {
            User newBtcUser = userRepository.findById(request.getBtcId())
                    .orElseThrow(() -> new RuntimeException("Tài khoản BTC không tồn tại"));
            Btc newBtcInfo = btcRepository.findById(newBtcUser.getId())
                    .orElseThrow(() -> new RuntimeException("Tài khoản này chưa được cấu hình thông tin BTC"));
            suKien.setBtc(newBtcUser);
            suKien.setDonVi(newBtcInfo.getDonVi());
        }

        SuKien updated = suKienRepository.save(suKien);
        log.info("Admin updated event: {}", updated.getTieuDe());

        return convertToDTO(updated);
    }

    /**
     * Duyệt sự kiện
     */
    @Transactional
    @LogActivity(action = "APPROVE_EVENT", description = "Admin duyệt sự kiện", targetTable = "su_kien")
    public void approveEvent(Integer id, String adminUsername) {
        SuKien suKien = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        suKien.setTrangThai(TrangThaiSuKien.APPROVED);
        suKien.setAdmin(admin);
        suKien.setLiDo(null);
        suKienRepository.save(suKien);

        log.info("Admin {} approved event: {}", adminUsername, suKien.getTieuDe());
    }

    /**
     * Từ chối sự kiện
     */
    @Transactional
    @LogActivity(action = "REJECT_EVENT", description = "Admin từ chối sự kiện", targetTable = "su_kien")
    public void rejectEvent(Integer id, String reason, String adminUsername) {
        SuKien suKien = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        suKien.setTrangThai(TrangThaiSuKien.REJECTED);
        suKien.setAdmin(admin);
        suKien.setLiDo(reason);
        suKienRepository.save(suKien);

        log.info("Admin {} rejected event: {} - Reason: {}", adminUsername, suKien.getTieuDe(), reason);
    }

    /**
     * Hủy sự kiện
     */
    @Transactional
    @LogActivity(action = "CANCEL_EVENT", description = "Admin hủy sự kiện", targetTable = "su_kien")
    public void cancelEvent(Integer id, String adminUsername) {
        SuKien suKien = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        suKien.setTrangThai(TrangThaiSuKien.CANCELLED);
        suKien.setAdmin(admin);
        suKienRepository.save(suKien);

        log.info("Admin {} cancelled event: {}", adminUsername, suKien.getTieuDe());
    }

    /**
     * Xóa vĩnh viễn sự kiện
     */
    @Transactional
    @LogActivity(action = "DELETE_EVENT", description = "Admin xóa vĩnh viễn sự kiện", targetTable = "su_kien")
    public void deleteEvent(Integer id) {
        SuKien suKien = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện cần xóa"));

        dangKySuKienRepository.deleteAllBySuKienId(id);
        suKienRepository.delete(suKien);

        log.info("Admin deleted event permanently: {}", suKien.getTieuDe());
    }

    /**
     * Đếm tổng số sự kiện
     */
    public long getTotalEvents() {
        return suKienRepository.count();
    }

    // ========== HELPER METHODS ==========

    /**
     * Validate logic thời gian sự kiện
     */
    private void validateEventDates(LocalDateTime start, LocalDateTime end,
                                    LocalDateTime openReg, LocalDateTime closeReg) {
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu sự kiện.");
        }

        if (closeReg.isBefore(openReg) || closeReg.isEqual(openReg)) {
            throw new IllegalArgumentException("Thời gian đóng đăng ký phải sau thời gian mở đăng ký.");
        }

        if (openReg.isAfter(start)) {
            throw new IllegalArgumentException("Thời gian mở đăng ký không được trễ hơn thời gian bắt đầu sự kiện.");
        }

        if (closeReg.isAfter(end)) {
            throw new IllegalArgumentException("Thời gian đóng đăng ký không được trễ hơn thời gian kết thúc sự kiện.");
        }
    }

    /**
     * Convert Entity sang DTO
     */
    private EventDTO convertToDTO(SuKien suKien) {
        EventDTO dto = new EventDTO();
        dto.setId(suKien.getId());
        dto.setTenSuKien(suKien.getTieuDe());
        dto.setMoTa(suKien.getMoTa());
        dto.setNoiDung(suKien.getNoiDung());
        dto.setPosterUrl(suKien.getPosterUrl());
        dto.setThoiGianBatDau(suKien.getThoigianBatdau());
        dto.setThoiGianKetThuc(suKien.getThoigianKetthuc());
        dto.setThoiGianMoDangKy(suKien.getThoigianMoDangky());
        dto.setThoiGianDongDangKy(suKien.getThoigianDongDangky());
        dto.setDiaDiem(suKien.getDiaDiem());
        dto.setGioiHanDangKy(suKien.getGioiHan());
        dto.setDiemRenLuyen(suKien.getDiemCong());
        dto.setDiemToiThieu(suKien.getDiemThoiThieu());
        dto.setTrangThai(suKien.getTrangThai());
        dto.setCreatedAt(suKien.getThoigianTao());
        dto.setUpdatedAt(suKien.getThoigianCapnhat());
        dto.setRejectionReason(suKien.getLiDo());

        if (suKien.getDonVi() != null) {
            dto.setDonViId(suKien.getDonVi().getId());
            dto.setDonViName(suKien.getDonVi().getTenDayDu());
        }

        if (suKien.getBtc() != null) {
            dto.setBtcUsername(suKien.getBtc().getUsername());
            if (suKien.getBtc().getBtc() != null) {
                dto.setBtcName(suKien.getBtc().getBtc().getTen());
            }
            dto.setCreatedBy(suKien.getBtc().getUsername());
        }

        if (suKien.getAdmin() != null) {
            dto.setAdminUsername(suKien.getAdmin().getUsername());
        }

        try {
            long count = dangKySuKienRepository.countBySuKien(suKien);
            dto.setSoLuongDaDangKy((int) count);
        } catch (Exception e) {
            dto.setSoLuongDaDangKy(0);
        }

        if (suKien.getTrangThai() == TrangThaiSuKien.PENDING) {
            if (suKien.getThoigianTao() != null &&
                    suKien.getThoigianTao().isAfter(LocalDateTime.now().minusDays(1))) {
                dto.setNewRequest(true);
            }
        }

        return dto;
    }

    /**
     * Lấy chi tiết sự kiện cho Modal (Admin)
     */
    public EventDTO getEventDetail(Integer id) {
        SuKien sk = suKienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện ID: " + id));

        // 1. Lấy tên BTC an toàn (Tránh lỗi null)
        String tenBtc = "Chưa cập nhật";
        if (sk.getBtc() != null) {
            // Ưu tiên lấy tên trong bảng Btc, nếu không có thì lấy display name của User, cuối cùng là username
            if (sk.getBtc().getBtc() != null && sk.getBtc().getBtc().getTen() != null) {
                tenBtc = sk.getBtc().getBtc().getTen();
            } else if (sk.getBtc().getHoTen() != null) {
                tenBtc = sk.getBtc().getHoTen();
            } else {
                tenBtc = sk.getBtc().getUsername();
            }
        }

        // 2. Lấy tên Đơn vị an toàn
        String tenDonVi = "Chưa cập nhật";
        if (sk.getDonVi() != null) {
            tenDonVi = sk.getDonVi().getTenDayDu();
        }

        // 3. Map sang EventDTO (Lưu ý tên trường phải khớp với file EventDTO.java)
        return EventDTO.builder()
                .id(sk.getId())
                .tenSuKien(sk.getTieuDe())        // Map tieuDe -> tenSuKien
                .moTa(sk.getMoTa())
                .noiDung(sk.getNoiDung())
                .diaDiem(sk.getDiaDiem())
                .posterUrl(sk.getPosterUrl())
                .thoiGianBatDau(sk.getThoigianBatdau()) // Map thoigianBatdau -> thoiGianBatDau
                .thoiGianKetThuc(sk.getThoigianKetthuc())
                .gioiHanDangKy(sk.getGioiHan())   // Map gioiHan -> gioiHanDangKy
                .diemCong(sk.getDiemCong())
                .trangThai(sk.getTrangThai())
                .btcName(tenBtc)
                .donViName(tenDonVi)
                .build();
    }
}