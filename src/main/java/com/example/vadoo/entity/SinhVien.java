package com.example.vadoo.entity;

import com.example.vadoo.entity.DonVi;
import com.example.vadoo.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sinh_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinhVien {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "mssv", nullable = false, unique = true, length = 20)
    private String mssv;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @Column(name = "gioi_tinh")
    private GioiTinh gioiTinh;

    @Column(name = "ten_lop", length = 50)
    private String tenLop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donvi_id")
    private DonVi donVi;

    @OneToMany(mappedBy = "sinhVien", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventFeedback> feedbacks = new ArrayList<>();

    public enum GioiTinh {
        NAM, NU, KHAC
    }

    // Thêm helper method
    public void addFeedback(EventFeedback feedback) {
        feedbacks.add(feedback);
        feedback.setSinhVien(this);
    }

    public void removeFeedback(EventFeedback feedback) {
        feedbacks.remove(feedback);
        feedback.setSinhVien(null);
    }
}