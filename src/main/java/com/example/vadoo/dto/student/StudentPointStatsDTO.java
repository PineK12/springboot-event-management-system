package com.example.vadoo.dto.student;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentPointStatsDTO {
    private int totalPoints;        // Tổng điểm hiện tại
    private String rankName;        // Tên xếp loại (Xuất sắc, Giỏi...)
    private String rankColor;       // Màu sắc hiển thị (text-green-600, etc.)
    private String rankDescription; // Mô tả (VD: Cố gắng giữ vững phong độ!)

    // Học bổng
    private boolean isScholarshipEligible; // Đủ điều kiện chưa?
    private int pointsNeededForScholarship; // Còn thiếu bao nhiêu?
    private int scholarshipThreshold;       // Ngưỡng điểm học bổng (VD: 85)

    // Tiến độ phấn đấu (Lên cấp tiếp theo)
    private int nextRankThreshold;
    private int pointsToNextRank;
}