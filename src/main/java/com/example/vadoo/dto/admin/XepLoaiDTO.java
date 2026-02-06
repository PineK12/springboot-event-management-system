package com.example.vadoo.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XepLoaiDTO {
    private Integer id;
    private String tenXepLoai;
    private Integer minPoint;
    private Integer maxPoint;
    private Integer hocKyId;
}