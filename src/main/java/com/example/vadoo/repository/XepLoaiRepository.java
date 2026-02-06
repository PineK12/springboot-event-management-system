package com.example.vadoo.repository;

import com.example.vadoo.entity.XepLoai;
import com.example.vadoo.entity.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface XepLoaiRepository extends JpaRepository<XepLoai, Integer> {

    List<XepLoai> findByHocKy(HocKy hocKy);

    @Query("SELECT x FROM XepLoai x WHERE x.hocKy.id = :hocKyId ORDER BY x.minPoint DESC")
    List<XepLoai> findByHocKyIdOrderByMinPointDesc(@Param("hocKyId") Integer hocKyId);

    // Tìm xếp loại dựa vào điểm
    @Query("SELECT x FROM XepLoai x WHERE x.hocKy.id = :hocKyId " +
            "AND :diem BETWEEN x.minPoint AND x.maxPoint")
    Optional<XepLoai> findByHocKyIdAndDiem(@Param("hocKyId") Integer hocKyId,
                                           @Param("diem") Integer diem);

    // Tìm xếp loại phù hợp với điểm số hiện tại (VD: điểm 75 nằm giữa 70 và 84)
    @Query("SELECT x FROM XepLoai x WHERE :point BETWEEN x.minPoint AND x.maxPoint")
    Optional<XepLoai> findByPoint(@Param("point") Integer point);

    // Tìm xếp loại tiếp theo (để tính phấn đấu)
    // Lấy xếp loại có min_point > điểm hiện tại, sắp xếp tăng dần, lấy cái đầu tiên
    @Query("SELECT x FROM XepLoai x WHERE x.minPoint > :point ORDER BY x.minPoint ASC LIMIT 1")
    Optional<XepLoai> findNextRank(@Param("point") Integer point);

    List<XepLoai> findAllByOrderByMinPointAsc();

    // Tìm xếp loại theo tên (để lấy ngưỡng điểm của loại 'Tốt')
    Optional<XepLoai> findByTenXepLoai(String tenXepLoai);
}