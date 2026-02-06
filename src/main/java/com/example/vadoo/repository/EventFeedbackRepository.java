package com.example.vadoo.repository;

import com.example.vadoo.entity.EventFeedback;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.entity.SinhVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {

    // Tìm feedback theo sự kiện
    List<EventFeedback> findBySuKien(SuKien suKien);

    Page<EventFeedback> findBySuKien(SuKien suKien, Pageable pageable);

    // Tìm feedback theo sinh viên
    List<EventFeedback> findBySinhVien(SinhVien sinhVien);

    // Kiểm tra sinh viên đã feedback cho sự kiện chưa
    Optional<EventFeedback> findBySuKienAndSinhVien(SuKien suKien, SinhVien sinhVien);

    boolean existsBySuKienAndSinhVien(SuKien suKien, SinhVien sinhVien);

    // Đếm số feedback của sự kiện
    long countBySuKien(SuKien suKien);

    // Lấy rating trung bình của sự kiện
    @Query("SELECT AVG(f.rating) FROM EventFeedback f WHERE f.suKien = :suKien")
    Double getAverageRatingBySuKien(@Param("suKien") SuKien suKien);

    // Đếm feedback theo rating
    @Query("SELECT COUNT(f) FROM EventFeedback f WHERE f.suKien = :suKien AND f.rating = :rating")
    long countBySuKienAndRating(@Param("suKien") SuKien suKien, @Param("rating") Integer rating);

    // Lấy feedback chưa có reply
    @Query("SELECT f FROM EventFeedback f WHERE f.suKien = :suKien AND f.replyContent IS NULL")
    List<EventFeedback> findBySuKienAndNoReply(@Param("suKien") SuKien suKien);

    @Query("SELECT f FROM EventFeedback f WHERE f.suKien = :suKien AND f.replyContent IS NULL")
    Page<EventFeedback> findBySuKienAndNoReply(@Param("suKien") SuKien suKien, Pageable pageable);

    // Lấy feedback đã có reply
    @Query("SELECT f FROM EventFeedback f WHERE f.suKien = :suKien AND f.replyContent IS NOT NULL")
    List<EventFeedback> findBySuKienAndHasReply(@Param("suKien") SuKien suKien);

    // Lấy feedback theo khoảng rating
    @Query("SELECT f FROM EventFeedback f WHERE f.suKien = :suKien AND f.rating >= :minRating")
    List<EventFeedback> findBySuKienAndRatingGreaterThanEqual(
            @Param("suKien") SuKien suKien,
            @Param("minRating") Integer minRating);

    // Lấy feedback gần đây của BTC
    @Query("SELECT f FROM EventFeedback f " +
            "JOIN f.suKien s " +
            "WHERE s.btc.id = :btcId " +
            "ORDER BY f.thoigianTao DESC")
    List<EventFeedback> findRecentFeedbacksByBtc(@Param("btcId") Integer btcId, Pageable pageable);

    // Thống kê phân bố rating của sự kiện
    @Query("SELECT f.rating, COUNT(f) FROM EventFeedback f " +
            "WHERE f.suKien = :suKien " +
            "GROUP BY f.rating " +
            "ORDER BY f.rating DESC")
    List<Object[]> getRatingDistributionBySuKien(@Param("suKien") SuKien suKien);

    // Thống kê feedback theo tháng cho BTC
    @Query("SELECT MONTH(f.thoigianTao), COUNT(f), AVG(f.rating) " +
            "FROM EventFeedback f " +
            "JOIN f.suKien s " +
            "WHERE s.btc.id = :btcId " +
            "AND YEAR(f.thoigianTao) = :year " +
            "GROUP BY MONTH(f.thoigianTao) " +
            "ORDER BY MONTH(f.thoigianTao)")
    List<Object[]> getMonthlyFeedbackStatsByBtc(
            @Param("btcId") Integer btcId,
            @Param("year") Integer year);


    // (Optional) Lấy điểm trung bình của 1 sự kiện cụ thể
    @Query("SELECT AVG(f.rating) FROM EventFeedback f WHERE f.suKien.id = :eventId")
    Double getAverageRatingBySuKienId(@Param("eventId") Integer eventId);

    /**
     * Lấy feedback theo BTC (Sắp xếp mới nhất)
     * SỬA: Dùng f.thoigianTao
     */
    @Query("SELECT f FROM EventFeedback f " +
            "WHERE f.suKien.btc.id = :btcId " +
            "ORDER BY f.thoigianTao DESC")
    List<EventFeedback> findAllByBtcId(@Param("btcId") Integer btcId);

    /**
     * Lấy feedback theo Sự kiện
     * SỬA: Dùng f.thoigianTao
     */
    @Query("SELECT f FROM EventFeedback f " +
            "WHERE f.suKien.id = :eventId " +
            "ORDER BY f.thoigianTao DESC")
    List<EventFeedback> findByEventId(@Param("eventId") Integer eventId);

    // Tính điểm trung bình
    @Query("SELECT AVG(f.rating) FROM EventFeedback f WHERE f.suKien.btc.id = :btcId")
    Double getAverageRatingByBtc(@Param("btcId") Integer btcId);

    /**
     * Tìm đánh giá cụ thể của 1 sinh viên cho 1 sự kiện
     * Dùng để hiển thị lịch sử xem sinh viên đã đánh giá chưa
     */
    Optional<EventFeedback> findBySinhVienUserIdAndSuKienId(Integer userId, Integer eventId);

    // 1. Tính điểm trung bình toàn hệ thống
    // COALESCE để xử lý trường hợp chưa có đánh giá nào -> trả về 0.0
    @Query("SELECT COALESCE(AVG(f.rating), 0.0) FROM EventFeedback f WHERE f.rating > 0")
    Double getGlobalAverageRating();

    // Tính điểm trung bình của TẤT CẢ sự kiện do 1 BTC tổ chức
    @Query("SELECT COALESCE(AVG(f.rating), 0.0) FROM EventFeedback f " +
            "WHERE f.suKien.btc.id = :btcId AND f.rating > 0")
    Double getAverageRatingByBtcId(@Param("btcId") Integer btcId);
}