package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "btc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Btc {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donvi_id", nullable = false)
    private DonVi donVi;

    @Column(name = "chuc_vu", length = 100)
    private String chucVu;
}