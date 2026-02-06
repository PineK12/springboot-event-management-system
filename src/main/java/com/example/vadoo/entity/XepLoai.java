package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "xep_loai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XepLoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_xeploai")
    private Integer id;

    @Column(name = "ten_xeploai", nullable = false, length = 50)
    private String tenXepLoai;

    @Column(name = "min_point", nullable = false)
    private Integer minPoint;

    @Column(name = "max_point", nullable = false)
    private Integer maxPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hocky_id")
    private HocKy hocKy;
}