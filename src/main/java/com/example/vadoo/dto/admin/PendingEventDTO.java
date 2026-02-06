package com.example.vadoo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingEventDTO {

    private Integer id;
    private String tieuDe;          // Tên sự kiện
    private String btcName;         // Tên BTC (Ban tổ chức)
    private String donViName;       // Tên đơn vị
    private String ngayTao;         // Ngày tạo (format: dd/MM/yyyy)
    private String trangThai;       // Trạng thái hiển thị
    private String trangThaiClass;  // CSS class cho badge trạng thái
}