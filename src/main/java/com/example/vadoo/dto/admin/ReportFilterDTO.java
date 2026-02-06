package com.example.vadoo.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterDTO {
    private Integer hocKyId;
    private String reportType;  // "good" hoặc "support"
    private Integer minDiem;     // Điểm tối thiểu để lọc
}