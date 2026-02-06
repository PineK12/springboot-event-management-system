// File: src/main/java/com/example/vadoo/dto/admin/EventRequestDTO.java

package com.example.vadoo.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequestDTO {
    private String tenSuKien;
    private String moTa;
    private String noiDung;
    private String diaDiem;
    private String posterUrl; // Thêm trường này

    private Integer btcId;

    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianKetThuc;
    private LocalDateTime thoiGianMoDangKy;
    private LocalDateTime thoiGianDongDangKy;

    private Integer gioiHanDangKy;
    private Integer diemRenLuyen; // Map với diemCong trong Entity
    private Integer diemThoiThieu;
}