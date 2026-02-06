package com.example.vadoo.dto.btc;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationDTO {
    private Long id;                // ID đăng ký (để update status)
    private String mssv;
    private String hoTen;
    private String lop;
    private LocalDateTime thoiGianDangKy;
    private LocalDateTime thoiGianCheckIn;
    private String trangThai;       // REGISTERED, PRESENT, ABSENT
    private Integer diem;           // Điểm nhận được
}