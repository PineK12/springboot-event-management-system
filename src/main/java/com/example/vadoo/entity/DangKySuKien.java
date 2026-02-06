package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dangky_sukien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DangKySuKien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dangky")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sukien_id", nullable = false)
    private SuKien suKien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinhvien_id", nullable = false)
    private SinhVien sinhVien;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    @Builder.Default
    private TrangThaiDangKy trangThai = TrangThaiDangKy.REGISTERED;

    @CreationTimestamp
    @Column(name = "thoigian_dangky", updatable = false)
    private LocalDateTime thoigianDangky;

    @Column(name = "qr_random_string", unique = true, length = 64)
    private String qrRandomString;

    @Column(name = "thoigian_checkin")
    private LocalDateTime thoigianCheckin;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangthai_thamgia")
    @Builder.Default
    private TrangThaiThamGia trangthaiThamgia = TrangThaiThamGia.ABSENT;

    @Column(name = "diem_nhan_duoc")
    @Builder.Default
    private Integer diemNhanDuoc = 0;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "ly_do_huy", columnDefinition = "TEXT")
    private String lyDoHuy;

    public enum TrangThaiDangKy {
        REGISTERED,
        CANCELLED_BY_USER,
        CANCELLED_BY_SYSTEM
    }

    public enum TrangThaiThamGia {
        PRESENT,
        ABSENT,
        LATE
    }
}