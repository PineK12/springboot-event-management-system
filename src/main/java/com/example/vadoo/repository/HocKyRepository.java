package com.example.vadoo.repository;

import com.example.vadoo.entity.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HocKyRepository extends JpaRepository<HocKy, Integer> {

    Optional<HocKy> findByIsCurrent(Boolean isCurrent);

    @Query("SELECT h FROM HocKy h ORDER BY h.startDate DESC")
    java.util.List<HocKy> findAllOrderByStartDateDesc();
}