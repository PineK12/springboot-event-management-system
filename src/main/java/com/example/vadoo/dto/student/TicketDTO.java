package com.example.vadoo.dto.student;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketDTO {
    private Long dangKyId;          // ID bản ghi đăng ký
    private Integer eventId;        // ID sự kiện
    private String tenSuKien;
    private String diaDiem;
    private LocalDateTime thoiGianBatDau;
    private String qrData;          // Chuỗi để tạo QR
    private String trangThaiCheckIn; // PRESENT, ABSENT...
    private Integer diemDuocNhan;    // Điểm sự kiện này mang lại
    private boolean isPast;          // Sự kiện đã qua chưa?
}