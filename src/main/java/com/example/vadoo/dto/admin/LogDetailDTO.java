package com.example.vadoo.dto.admin;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogDetailDTO {
    private Long logId;
    private String action;
    private String userName;
    private String userRole;
    private String targetName;
    private String time;
    private String description;

    // Parsed JSON
    private Map<String, Object> oldValueMap;
    private Map<String, Object> newValueMap;
}