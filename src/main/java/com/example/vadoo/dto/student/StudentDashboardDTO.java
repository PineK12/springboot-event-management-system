package com.example.vadoo.dto.student;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StudentDashboardDTO {
    // Thông tin sinh viên
    private String hoTen;
    private String maSinhVien;
    private int diemRenLuyen; // Tổng điểm
    private int diemCongThangNay; // Điểm tăng trong tháng

    // Dữ liệu sự kiện
    private int soSuKienSapToi; // Số sự kiện đã đăng ký sắp diễn ra
    private List<StudentEventDTO> eventsRegistered; // Danh sách sự kiện đã đăng ký (Top 3)
    private List<StudentEventDTO> eventsSuggested;  // Danh sách sự kiện gợi ý (Top 3)
}