package com.example.vadoo.dto.btc;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackDTO {
    private Long id;                // ID của feedback (để gửi reply)
    private String studentName;     // Tên sinh viên
    private String studentAvatarChar; // Chữ cái đầu của tên (để hiển thị avatar)
    private String eventName;       // Tên sự kiện
    private int rating;             // Điểm sao
    private String comment;         // Nội dung đánh giá
    private LocalDateTime createdAt; // Thời gian đánh giá

    // --- BỔ SUNG TRƯỜNG TRẢ LỜI ---
    private String replyContent;    // Nội dung BTC trả lời
    private LocalDateTime replyTime;// Thời gian trả lời

    // --- QUAN TRỌNG: THÊM DÒNG NÀY ĐỂ SỬA LỖI ---
    private String repliedByName;   // Tên cán bộ BTC đã trả lời
}