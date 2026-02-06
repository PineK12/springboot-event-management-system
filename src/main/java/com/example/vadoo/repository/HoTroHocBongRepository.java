package com.example.vadoo.repository;

import com.example.vadoo.entity.HoTroHocBong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoTroHocBongRepository extends JpaRepository<HoTroHocBong, Integer> {

    List<HoTroHocBong> findByIsActive(Boolean isActive);

    @Query("SELECT h FROM HoTroHocBong h WHERE h.isActive = true ORDER BY h.minDrlRequired DESC")
    List<HoTroHocBong> findAllActiveOrderByMinDrlDesc();

    // Tìm học bổng "dễ nhất" đang active (mức thấp nhất để phấn đấu)
    // Hoặc tìm học bổng có mức điểm cao nhất mà user chưa đạt được
    @Query("SELECT h FROM HoTroHocBong h WHERE h.isActive = true ORDER BY h.minDrlRequired ASC LIMIT 1")
    Optional<HoTroHocBong> findBaseScholarship();

    // Lấy gói hỗ trợ có yêu cầu điểm THẤP NHẤT đang kích hoạt
    // Để làm ngưỡng mặc định cho báo cáo hỗ trợ
    HoTroHocBong findFirstByIsActiveTrueOrderByMinDrlRequiredAsc();
}