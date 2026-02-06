package com.example.vadoo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceDTO {

    private Long dangKyId;           // ID đăng ký
    private String mssv;
    private String ten;
    private String tenLop;
    private String email;
    private String sdt;

    private Boolean daDiemDanh;         // Đã điểm danh hay chưa
    private LocalDateTime thoiGianDiemDanh;
    private LocalDateTime thoiGianDangKy;

    private String ghiChu;
}