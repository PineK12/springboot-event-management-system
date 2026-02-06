package com.example.vadoo.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPointDTO {
    private Integer userId;
    private String mssv;
    private String ten;
    private String tenLop;
    private String khoaName;
    private Integer soSuKien;          // Số sự kiện đã tham gia
    private Integer diemTong;           // Tổng điểm DRL
    private String xepLoai;             // Xuất sắc, Tốt, Khá, TB, Yếu
    private Integer hocKyId;
}