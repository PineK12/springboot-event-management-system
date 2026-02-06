package com.example.vadoo.dto.btc;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BTCEventSummaryDTO {
    private Integer eventId;
    private String tieuDe;
    private LocalDateTime thoigianBatdau;
    private LocalDateTime thoigianKetthuc;
    private int currentRegistrations; // Số người đã đăng ký
    private int gioiHan;              // Giới hạn số lượng
    private String trangThai;         // APPROVED, PENDING...
    private int registrationPercent;  // % lấp đầy (để vẽ thanh progress nếu cần)
    private boolean isRunning;        // Cờ đánh dấu sự kiện đang chạy
}