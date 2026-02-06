package com.example.vadoo.dto.admin;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogFilterDTO {
    private String keyword;            // Search trong user, action, target
    private LocalDate startDate;
    private LocalDate endDate;
    private String actionType;         // all, create, update, delete, auth
    private Integer page;
    private Integer size;
}