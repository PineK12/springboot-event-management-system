package com.example.vadoo.dto.admin;

import com.example.vadoo.entity.SuKien.TrangThaiSuKien;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {

    private Integer id;
    private String tenSuKien;
    private String moTa;
    private String noiDung;
    private String posterUrl;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private LocalDateTime thoiGianMoDangKy;
    private LocalDateTime thoiGianDongDangKy;
    private String diaDiem;

    private Integer gioiHanDangKy;
    private Integer diemRenLuyen;
    private Integer diemToiThieu;
    private Integer diemCong;
    private TrangThaiSuKien trangThai;

    private Integer donViId;
    private String donViName;

    private String createdBy;
    private String btcUsername;
    private String btcName;

    private String adminUsername;
    private String adminName;

    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean isNewRequest;
    private boolean isUpdateRequest;

    private Integer soLuongDaDangKy;

    public String getTrangThaiHienThi() {
        // 1. Nếu chưa duyệt hoặc bị hủy/từ chối -> Trả về đúng trạng thái đó
        if (trangThai != TrangThaiSuKien.APPROVED) {
            return trangThai.name();
        }

        // 2. Nếu đã duyệt -> Tính toán dựa trên thời gian
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(thoiGianBatDau)) {
            return "UPCOMING"; // Sắp diễn ra
        } else if (now.isAfter(thoiGianKetThuc)) {
            return "ENDED";    // Đã kết thúc
        } else {
            return "HAPPENING"; // Đang diễn ra
        }
    }
}