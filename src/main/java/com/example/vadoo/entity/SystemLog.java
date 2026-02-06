package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "target_table", length = 50)
    private String targetTable;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "old_value", columnDefinition = "LONGTEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "LONGTEXT")
    private String newValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 20)
    private String status = "SUCCESS";

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "thoigian_tao")
    private LocalDateTime thoigianTao;

    @PrePersist
    protected void onCreate() {
        thoigianTao = LocalDateTime.now();
    }
}