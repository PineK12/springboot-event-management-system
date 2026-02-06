package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "su_kien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuKien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sukien")
    private Integer id;

    @Column(name = "tieu_de", nullable = false)
    private String tieuDe;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "noi_dung", columnDefinition = "LONGTEXT")
    private String noiDung;

    @Column(name = "poster_url", columnDefinition = "LONGTEXT")
    private String posterUrl;

    @Column(name = "thoigian_batdau", nullable = false)
    private LocalDateTime thoigianBatdau;

    @Column(name = "thoigian_ketthuc", nullable = false)
    private LocalDateTime thoigianKetthuc;

    @Column(name = "thoigian_mo_dangky", nullable = false)
    private LocalDateTime thoigianMoDangky;

    @Column(name = "thoigian_dong_dangky", nullable = false)
    private LocalDateTime thoigianDongDangky;

    @Column(name = "dia_diem")
    private String diaDiem;

    @Column(name = "gioi_han")
    @Builder.Default
    private Integer gioiHan = 0;

    @Column(name = "diem_cong")
    @Builder.Default
    private Integer diemCong = 0;

    @Column(name = "diem_thoi_thieu")
    @Builder.Default
    private Integer diemThoiThieu = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    @Builder.Default
    private TrangThaiSuKien trangThai = TrangThaiSuKien.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "btc_id", nullable = false)
    private User btc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donvi_id", nullable = false)
    private DonVi donVi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(name = "lido", columnDefinition = "TEXT")
    private String liDo;

    @CreationTimestamp
    @Column(name = "thoigian_tao", updatable = false)
    private LocalDateTime thoigianTao;

    @UpdateTimestamp
    @Column(name = "thoigian_capnhat")
    private LocalDateTime thoigianCapnhat;

    @OneToMany(mappedBy = "suKien", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventFeedback> feedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "suKien", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // Tránh vòng lặp vô hạn khi toString
    @EqualsAndHashCode.Exclude
    private List<DangKySuKien> dangKySuKiens = new ArrayList<>();

    public enum TrangThaiSuKien {
        DRAFT,
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED,
        COMPLETED
    }

    // Thêm helper method
    public void addFeedback(EventFeedback feedback) {
        feedbacks.add(feedback);
        feedback.setSuKien(this);
    }

    public void removeFeedback(EventFeedback feedback) {
        feedbacks.remove(feedback);
        feedback.setSuKien(null);
    }
}