package com.example.vadoo.dto.student;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StudentEventDTO {
    private Integer id;
    private String tieuDe;
    private String posterUrl;
    private String diaDiem;
    private String moTa;

    // Thêm các trường thời gian để filter cho chuẩn
    private LocalDateTime thoigianMoDangky;
    private LocalDateTime thoigianDongDangky;
    private LocalDateTime thoigianBatdau;
    private LocalDateTime thoigianKetthuc;

    private boolean canCancel;
    private boolean registered;

    private String donViName;   // Tên đơn vị tổ chức (CLB, Khoa...)
    private Integer diemCong;   // Điểm rèn luyện

    private Integer gioiHan;    // Giới hạn số lượng
    private Integer daDangKy;   // Số lượng đã đăng ký hiện tại

    // Trạng thái logic
    private boolean isRegistered; // Sinh viên đã đăng ký chưa?
}