package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Integer id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "pass", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "sdt", length = 20)
    private String sdt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "thoigian_tao", updatable = false)
    private LocalDateTime thoigianTao;

    @UpdateTimestamp
    @Column(name = "thoigian_capnhat")
    private LocalDateTime thoigianCapnhat;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SinhVien sinhVien;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Btc btc;

    /**
     * Lấy tên hiển thị của user (từ SinhVien hoặc Btc)
     */
    public String getHoTen() {
        if (sinhVien != null) {
            return sinhVien.getTen();
        } else if (btc != null) {
            return btc.getTen();
        }
        return username; // Fallback nếu không có
    }

    /**
     * Lấy tên đơn vị của user
     */
    public String getDonViName() {
        if (sinhVien != null && sinhVien.getDonVi() != null) {
            return sinhVien.getDonVi().getTenDayDu();
        } else if (btc != null && btc.getDonVi() != null) {
            return btc.getDonVi().getTenDayDu();
        }
        return null;
    }
}