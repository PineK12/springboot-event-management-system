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
public class CategoryStatsDTO {

    private List<String> labels;      // ["Hội thảo", "Workshop", ...]
    private List<Integer> data;       // [35, 25, 20, ...]
}