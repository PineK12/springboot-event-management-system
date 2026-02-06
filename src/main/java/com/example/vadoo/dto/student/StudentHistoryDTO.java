package com.example.vadoo.dto.student;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StudentHistoryDTO {
    private Long id;                // ID đăng ký (để update rating)
    private String eventTitle;      // Tên sự kiện
    private String donViName;       // Tên đơn vị tổ chức
    private String eventShortName;  // Tên viết tắt (để làm avatar)
    private LocalDateTime eventTime;
    private String diaDiem;
    private String attendanceStatus; // PRESENT, ABSENT
    private Integer diemCong;
    private Integer userRating;      // Điểm đánh giá của user (nếu có)
    private String userComment;      // Comment của user

    // --- BỔ SUNG CÁC TRƯỜNG NÀY ---
    private String replyContent;    // Nội dung BTC trả lời
    private String repliedByName;   // Tên BTC trả lời
    private LocalDateTime replyTime;// Thời gian trả lời
    // ------------------------------
}