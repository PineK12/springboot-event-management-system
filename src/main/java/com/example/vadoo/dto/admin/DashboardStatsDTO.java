package com.example.vadoo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private long totalUsers;        // Tổng số user
    private long activeEvents;      // Số sự kiện đang active (APPROVED)
    private long pendingEvents;     // Số sự kiện chờ duyệt (PENDING)
    private long totalPoints;       // Tổng điểm rèn luyện đã cộng

    /**
     * Format số điểm thành dạng K (ví dụ: 89000 -> 89K)
     */
    public String getFormattedTotalPoints() {
        if (totalPoints >= 1000) {
            return (totalPoints / 1000) + "K";
        }
        return String.valueOf(totalPoints);
    }
}