package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotro_hocbong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoTroHocBong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hotro")
    private Integer id;

    @Column(name = "ten_hotro", nullable = false, length = 100)
    private String tenHoTro;

    @Column(name = "min_drl_required", nullable = false)
    private Integer minDrlRequired;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "so_tien")
    private Long soTien = 0L;

    @Column(name = "is_active")
    private Boolean isActive = true;
}