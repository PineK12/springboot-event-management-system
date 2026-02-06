package com.example.vadoo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "persistent_logins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersistentLogin {

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Id
    @Column(name = "series", length = 64)
    private String series;

    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
}