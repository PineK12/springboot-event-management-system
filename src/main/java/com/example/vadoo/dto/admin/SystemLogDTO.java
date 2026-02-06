package com.example.vadoo.dto.admin;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogDTO {
    private Long id;
    private String userName;           // Tên người thực hiện
    private String userRole;           // Vai trò: ADMIN, BTC, SINH_VIEN
    private String action;             // CREATE, UPDATE, DELETE, LOGIN...
    private String targetTable;        // Bảng bị tác động
    private String targetId;           // ID đối tượng
    private String targetName;         // Tên đối tượng (friendly)
    private String description;        // Mô tả ngắn
    private String status;             // SUCCESS, FAILED
    private String ipAddress;
    private LocalDateTime thoigianTao;

    // For detail modal
    private String oldValue;           // JSON string
    private String newValue;           // JSON string
}