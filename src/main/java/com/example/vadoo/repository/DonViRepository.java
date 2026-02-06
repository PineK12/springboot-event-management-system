package com.example.vadoo.repository;

import com.example.vadoo.entity.DonVi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // 👈 Import quan trọng
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonViRepository extends JpaRepository<DonVi, Integer> {

    // ✅ THÊM @Param để map đúng với :loaiDonVi
    @Query("SELECT d FROM DonVi d WHERE d.loaiDonVi = :loaiDonVi ORDER BY d.tenDayDu")
    List<DonVi> findByLoaiDonViOrderByTenDayDu(@Param("loaiDonVi") DonVi.LoaiDonVi loaiDonVi);

    // Spring tự generate query -> Không cần @Param
    List<DonVi> findByLoaiDonVi(DonVi.LoaiDonVi loaiDonVi);

    // Query không tham số -> Không cần @Param
    @Query("SELECT d FROM DonVi d WHERE d.parent IS NULL")
    List<DonVi> findRootDonVi();

    // ✅ THÊM @Param để map đúng với :parentId
    @Query("SELECT d FROM DonVi d WHERE d.parent.id = :parentId")
    List<DonVi> findByParentId(@Param("parentId") Integer parentId);

    List<DonVi> findByIsActiveTrue();

    List<DonVi> findByLoaiDonViAndIsActiveTrue(DonVi.LoaiDonVi loaiDonVi);

    @Query("SELECT d FROM DonVi d WHERE d.loaiDonVi = 'KHOA' AND d.isActive = true ORDER BY d.tenDayDu")
    List<DonVi> findKhoaForSinhVien();

    @Query("SELECT d FROM DonVi d WHERE d.loaiDonVi != 'KHOA' AND d.isActive = true ORDER BY d.tenDayDu")
    List<DonVi> findDonViForBTC();

    @Query("SELECT d FROM DonVi d WHERE d.isActive = true ORDER BY d.loaiDonVi, d.tenDayDu")
    List<DonVi> findAllOrdered();

    List<DonVi> findByParentIsNull();

    boolean existsByTenDayDu(String tenDayDu);

    boolean existsByTenDayDuAndIdNot(String tenDayDu, Integer id);

    @Query("SELECT d FROM DonVi d WHERE d.loaiDonVi = 'KHOA'")
    List<DonVi> findAllKhoa();
}