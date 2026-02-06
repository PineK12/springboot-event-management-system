package com.example.vadoo.dto;

import com.example.vadoo.entity.DonVi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonViDTO {

    private Integer id;

    @NotNull(message = "Loại đơn vị không được để trống")
    private DonVi.LoaiDonVi loaiDonVi;

    @NotBlank(message = "Tên đầy đủ không được để trống")
    private String tenDayDu;

    private Integer parentId;

    private Boolean isActive = true;
}