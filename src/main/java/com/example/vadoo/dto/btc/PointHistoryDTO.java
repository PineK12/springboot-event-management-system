package com.example.vadoo.dto.btc;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PointHistoryDTO {
    private String mssv;
    private String hoTen;
    private String tenLop;
    private String tenSuKien;
    private LocalDateTime thoiGianCheckIn;
    private Integer diem;
    private String trangThai;
}