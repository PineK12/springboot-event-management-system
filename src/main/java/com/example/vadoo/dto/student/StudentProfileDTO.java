package com.example.vadoo.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDTO {
    // Thông tin Read-only (Không cho sửa)
    private String mssv;
    private String lop;
    private String khoa; // Khoa/Viện

    // Thông tin Editable (Cho phép sửa)
    private String hoTen;
    private String email;
    private String sdt;
    private String avatarUrl;
    private LocalDate ngaySinh;
    private String gioiTinh;
}