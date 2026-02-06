package com.example.vadoo.dto.btc;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BTCEventDetailDTO {
    private Integer id;
    private String tieuDe;
    private String moTa;
    private String noiDung;      // Nội dung chi tiết (HTML)
    private String posterUrl;
    private String diaDiem;

    private LocalDateTime thoigianBatdau;
    private LocalDateTime thoigianKetthuc;
    private LocalDateTime thoigianMoDangky;
    private LocalDateTime thoigianDongDangky;

    private Integer gioiHan;
    private Integer diemCong;

    // Thống kê
    private int soLuongDangKy;
    private int soLuongCheckIn;

    // Trạng thái & Logic hiển thị
    private String trangThai;    // APPROVED, PENDING...
    private String liDoTuChoi;   // Nếu bị từ chối
    private boolean isRunning;   // Đang diễn ra?
    private boolean isEditable;  // Có được sửa không?
}