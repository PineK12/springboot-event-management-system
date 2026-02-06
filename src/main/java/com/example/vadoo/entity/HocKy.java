package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "hoc_ky")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hocky")
    private Integer id;

    @Column(name = "ten_hocky", nullable = false, length = 50)
    private String tenHocKy;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current")
    private Boolean isCurrent = false;
}