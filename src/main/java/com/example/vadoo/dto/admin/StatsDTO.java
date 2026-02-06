package com.example.vadoo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsDTO {

    // Thống kê tổng quan
    private Long totalEvents;           // Tổng số sự kiện (tháng này)
    private Long totalRegistrations;    // Tổng lượt đăng ký
    private Long totalAttendance;       // Tổng đã điểm danh
    private Double averageRating;       // Đánh giá trung bình
    private Integer attendanceRate;     // Tỷ lệ điểm danh (%)

    // Dữ liệu biểu đồ
    private MonthlyStatsDTO monthlyStats;
    private CategoryStatsDTO categoryStats;
}