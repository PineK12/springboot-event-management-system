package com.example.vadoo.repository;

import com.example.vadoo.entity.Btc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BtcRepository extends JpaRepository<Btc, Integer> {
}