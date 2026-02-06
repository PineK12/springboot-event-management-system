package com.example.vadoo.repository;

import com.example.vadoo.entity.SystemLog;
import com.example.vadoo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    // Tìm theo user
    Page<SystemLog> findByUser(User user, Pageable pageable);

    // Tìm theo action
    Page<SystemLog> findByAction(String action, Pageable pageable);

    // Tìm theo khoảng thời gian
    @Query("SELECT s FROM SystemLog s WHERE s.thoigianTao BETWEEN :startDate AND :endDate ORDER BY s.thoigianTao DESC")
    Page<SystemLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    Pageable pageable);

    // Search tổng hợp
    @Query("SELECT s FROM SystemLog s " +
            "LEFT JOIN FETCH s.user u " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "      LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(s.action) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:action IS NULL OR :action = 'all' OR s.action LIKE CONCAT(:action, '%')) " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR s.thoigianTao >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR s.thoigianTao <= :endDate) " +
            "ORDER BY s.thoigianTao DESC")
    Page<SystemLog> searchLogs(@Param("keyword") String keyword,
                               @Param("action") String action,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate,
                               Pageable pageable);

    // Lấy logs gần nhất
    @Query("SELECT s FROM SystemLog s LEFT JOIN FETCH s.user ORDER BY s.thoigianTao DESC")
    List<SystemLog> findTop100ByOrderByThoigianTaoDesc(Pageable pageable);

    // Đếm logs theo action
    long countByAction(String action);

    // Đếm logs theo user
    long countByUser(User user);

    // Xóa logs cũ hơn N ngày
    @Modifying // Bắt buộc phải có để thực hiện DELETE/UPDATE
    @Query("DELETE FROM SystemLog s WHERE s.thoigianTao < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Lấy logs gần đây của BTC
    @Query("SELECT sl FROM SystemLog sl " +
            "WHERE sl.user.id = :btcUserId " +
            "ORDER BY sl.thoigianTao DESC")
    List<SystemLog> findRecentLogsByBtcId(@Param("btcUserId") Integer btcUserId, Pageable pageable);

}