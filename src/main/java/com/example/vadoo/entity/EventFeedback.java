package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_feed")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sukien_id", nullable = false)
    private SuKien suKien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinhvien_id", nullable = false)
    private SinhVien sinhVien;

    @Column(name = "rating", columnDefinition = "TINYINT")
    private Integer rating;  // 1-5 stars

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "reply_content", columnDefinition = "TEXT")
    private String replyContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by")
    private User repliedBy;

    @Column(name = "thoigian_reply")
    private LocalDateTime thoigianReply;

    @Column(name = "thoigian_tao")
    private LocalDateTime thoigianTao;

    @PrePersist
    protected void onCreate() {
        thoigianTao = LocalDateTime.now();
    }
}