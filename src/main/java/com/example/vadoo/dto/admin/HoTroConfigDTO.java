package com.example.vadoo.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoTroConfigDTO {
    private Integer id;
    private String tenHoTro;
    private Integer minDrlRequired;
    private String moTa;
    private Long soTien;
    private Boolean isActive;
}