package com.example.vadoo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "don_vi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DonVi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_donvi")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ten_donvi", nullable = false)
    private LoaiDonVi loaiDonVi;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "ten_day_du", nullable = false)
    private String tenDayDu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DonVi parent;

    @CreationTimestamp
    @Column(name = "thoigian_tao", updatable = false)
    private LocalDateTime thoigianTao;

    public enum LoaiDonVi {
        KHOA,           // Khoa đào tạo (cho sinh viên)
        PHONG_BAN,      // Phòng ban (CTSV, Đào tạo,...)
        CLB,            // Câu lạc bộ
        DOAN_HOI
    }
}