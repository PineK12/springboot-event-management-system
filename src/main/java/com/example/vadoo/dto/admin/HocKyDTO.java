package com.example.vadoo.dto.admin;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HocKyDTO {
    private Integer id;
    private String tenHocKy;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}