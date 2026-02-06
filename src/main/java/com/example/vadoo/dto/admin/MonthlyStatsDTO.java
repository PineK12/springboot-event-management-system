package com.example.vadoo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatsDTO {

    private List<String> labels;      // ["T1", "T2", "T3", ...]
    private List<Integer> data;       // [320, 450, 380, ...]
}