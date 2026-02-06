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
public class EventStatsDTO {

    private Integer id;
    private String tenSuKien;
    private String category;            // Loại sự kiện (Hội thảo, Workshop...)

    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;

    private Integer soLuongDangKy;      // Số lượng đã đăng ký
    private Integer soLuongDiemDanh;    // Số lượng đã điểm danh
    private Integer gioiHan;            // Giới hạn đăng ký

    private Integer tyLeDiemDanh;       // Tỷ lệ điểm danh (%)
    private String status;              // upcoming, ongoing, completed
    private TrangThaiSuKien trangThai;

    private String donViName;
}