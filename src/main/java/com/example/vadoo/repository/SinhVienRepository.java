package com.example.vadoo.repository;

import com.example.vadoo.entity.SinhVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SinhVienRepository extends JpaRepository<SinhVien, Integer> {

    Optional<SinhVien> findByMssv(String mssv);

    boolean existsByMssv(String mssv);

    // Tìm kiếm kết hợp: Keyword (Mssv/Ten) + Khoa ID
    @Query("SELECT s FROM SinhVien s WHERE " +
            "(:khoaId IS NULL OR s.donVi.id = :khoaId) AND " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(s.mssv) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.ten) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<SinhVien> searchStudents(@Param("keyword") String keyword, @Param("khoaId") Integer khoaId);

    // --- BỔ SUNG HÀM NÀY ---
    @Query("SELECT sv FROM SinhVien sv " +
            "LEFT JOIN sv.donVi dv " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "       LOWER(sv.mssv) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "       LOWER(sv.ten) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:khoaId IS NULL OR dv.id = :khoaId)")
    List<SinhVien> searchSinhVien(@Param("keyword") String keyword,
                                  @Param("khoaId") Integer khoaId);
}