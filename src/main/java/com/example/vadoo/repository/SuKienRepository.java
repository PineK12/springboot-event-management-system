package com.example.vadoo.repository;

import com.example.vadoo.entity.SuKien;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuKienRepository extends JpaRepository<SuKien, Integer> {

    // ========== QUERIES CHO BTC DASHBOARD ==========

    /**
     * Đếm sự kiện ĐANG CHẠY của BTC
     * Logic: APPROVED + thời gian hiện tại nằm trong khoảng [batdau, ketthuc]
     */
    Integer countByBtcIdAndTrangThaiAndThoigianBatdauBeforeAndThoigianKetthucAfter(
            Integer btcId,
            SuKien.TrangThaiSuKien trangThai,
            LocalDateTime time1, // Dành cho BatdauBefore
            LocalDateTime time2  // Dành cho KetthucAfter (BẠN ĐANG THIẾU CÁI NÀY)
    );

    /**
     * Đếm sự kiện CHỜ DUYỆT của BTC
     */
    Integer countByBtcIdAndTrangThai(Integer btcId, SuKien.TrangThaiSuKien trangThai);

    /**
     * Lấy danh sách sự kiện SẮP DIỄN RA + ĐANG DIỄN RA
     * Logic: APPROVED hoặc PENDING + chưa kết thúc
     * Sắp xếp: ưu tiên sự kiện đang chạy, sau đó sự kiện sắp diễn ra
     */
    @Query("SELECT s FROM SuKien s " +
            "WHERE s.btc.id = :btcId " +
            "AND s.trangThai IN ('APPROVED', 'PENDING', 'REJECTED') " +
            "AND s.thoigianKetthuc >= :now " +
            "ORDER BY s.thoigianBatdau ASC")
    List<SuKien> findUpcomingEventsByBtc(
            @Param("btcId") Integer btcId,
            @Param("now") LocalDateTime now);

    /**
     * Lấy sự kiện gần đây của BTC (để hiển thị activity log)
     */
    @Query("SELECT s FROM SuKien s " +
            "WHERE s.btc.id = :btcId " +
            "ORDER BY s.thoigianCapnhat DESC")
    List<SuKien> findRecentEventsByBtc(@Param("btcId") Integer btcId, Pageable pageable);

    // ========== QUERIES CHO ADMIN DASHBOARD ==========

    /**
     * Đếm TẤT CẢ sự kiện đang hoạt động (ACTIVE)
     * Logic: APPROVED + đang trong thời gian diễn ra
     */
    @Query("SELECT COUNT(s) FROM SuKien s " +
            "WHERE s.trangThai = 'APPROVED' " +
            "AND s.thoigianBatdau <= CURRENT_TIMESTAMP " +
            "AND s.thoigianKetthuc >= CURRENT_TIMESTAMP")
    long countActiveEvents();

    /**
     * Đếm sự kiện theo trạng thái
     */
    long countByTrangThai(SuKien.TrangThaiSuKien trangThai);

    /**
     * Lấy sự kiện chờ duyệt gần nhất (cho admin dashboard)
     */
    @Query("SELECT s FROM SuKien s " +
            "LEFT JOIN FETCH s.btc " +
            "LEFT JOIN FETCH s.donVi " +
            "WHERE s.trangThai = 'PENDING' " +
            "ORDER BY s.thoigianTao DESC")
    List<SuKien> findTopPendingEvents();

    // ========== QUERIES CHO STATS SERVICE ==========

    /**
     * Đếm sự kiện hợp lệ trong khoảng thời gian
     * Valid = APPROVED hoặc COMPLETED
     * Đếm theo thời gian bắt đầu nằm trong khoảng [startDate, endDate]
     */
    @Query("SELECT COUNT(s) FROM SuKien s " +
            "WHERE s.trangThai IN ('APPROVED', 'COMPLETED') " +
            "AND s.thoigianBatdau BETWEEN :startDate AND :endDate")
    long countValidEventsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy tất cả sự kiện hợp lệ (APPROVED hoặc COMPLETED)
     * Dùng để phân loại theo đơn vị
     */
    @Query("SELECT s FROM SuKien s " +
            "LEFT JOIN FETCH s.donVi " +
            "WHERE s.trangThai IN ('APPROVED', 'COMPLETED')")
    List<SuKien> findAllValidEvents();

    /**
     * Lấy tất cả sự kiện với relations (để hiển thị danh sách)
     */
    @Query("SELECT s FROM SuKien s " +
            "LEFT JOIN FETCH s.btc " +
            "LEFT JOIN FETCH s.donVi " +
            "ORDER BY s.thoigianBatdau DESC")
    List<SuKien> findAllWithRelations();

    /**
     * Lấy sự kiện theo ID với relations
     */
    @Query("SELECT s FROM SuKien s " +
            "LEFT JOIN FETCH s.btc " +
            "LEFT JOIN FETCH s.donVi " +
            "WHERE s.id = :id")
    Optional<SuKien> findByIdWithRelations(@Param("id") Integer id);

    // Thêm hàm này: Tìm theo ID sự kiện VÀ ID của BTC (để tránh BTC này xem trộm sự kiện BTC kia)
    Optional<SuKien> findByIdAndBtcId(Integer id, Integer btcId);

    // Lấy tất cả sự kiện của BTC (để hiện ở trang Quản lý)
    List<SuKien> findByBtcIdOrderByThoigianTaoDesc(Integer btcId);

    // Hàm tìm kiếm kết hợp:
    // 1. Phải đúng BTC (btcId)
    // 2. Nếu có keyword -> Tìm theo Tiêu đề (không phân biệt hoa thường)
    // 3. Nếu có status -> Tìm theo Trạng thái (Nếu status null thì lấy hết)
    @Query("SELECT s FROM SuKien s WHERE s.btc.id = :btcId " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.tieuDe) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR s.trangThai = :status) " +
            "ORDER BY s.thoigianTao DESC")
    List<SuKien> searchEventsByBtc(@Param("btcId") Integer btcId,
                                   @Param("keyword") String keyword,
                                   @Param("status") SuKien.TrangThaiSuKien status);

    // Tìm sự kiện sắp tới (Đã duyệt)
    @Query("SELECT s FROM SuKien s WHERE s.trangThai = 'APPROVED' AND s.thoigianBatdau > :now ORDER BY s.thoigianBatdau ASC")
    List<SuKien> findUpcomingEvents(@Param("now") LocalDateTime now);

    // HÀM MỚI: Lấy sự kiện sắp tới, sắp xếp theo ĐIỂM CỘNG GIẢM DẦN (Cao nhất lên đầu)
    // Nếu điểm bằng nhau thì lấy cái nào diễn ra trước
    @Query("SELECT s FROM SuKien s " +
            "WHERE s.trangThai = 'APPROVED' " +
            "AND s.thoigianBatdau > :now " +
            "ORDER BY s.diemCong DESC, s.thoigianBatdau ASC")
    List<SuKien> findHighPointEvents(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM SuKien s " +
            "LEFT JOIN s.donVi d " +
            "WHERE s.trangThai = 'APPROVED' " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(s.tieuDe) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:topic IS NULL OR :topic = '' OR :topic = 'all' OR d.tenDayDu LIKE CONCAT('%', :topic, '%')) " +
            "ORDER BY s.thoigianBatdau ASC")
    List<SuKien> searchStudentEvents(@Param("keyword") String keyword,
                                     @Param("topic") String topic);

    @Query("SELECT s FROM SuKien s " +
            "WHERE s.btc.id = :btcId " +
            "AND s.trangThai = :status " +
            "AND s.thoigianKetthuc > :now " + // Chỉ lấy sự kiện chưa kết thúc
            "ORDER BY s.thoigianBatdau ASC")
    List<SuKien> findEventsForCheckin(@Param("btcId") Integer btcId,
                                      @Param("status") SuKien.TrangThaiSuKien status,
                                      @Param("now") LocalDateTime now);

    /**
     * Lấy tất cả sự kiện do BTC này tạo, sắp xếp ID giảm dần (Mới nhất lên đầu)
     * Dùng để hiển thị trong Dropdown lọc danh sách đăng ký
     */
    @Query("SELECT s FROM SuKien s WHERE s.btc.id = :btcId ORDER BY s.id DESC")
    List<SuKien> findByBtcIdOrderByIdDesc(@Param("btcId") Integer btcId);

    /**
     * Hàm MỚI: Lấy danh sách sự kiện theo trạng thái, sắp xếp sự kiện mới nhất lên đầu
     * Dùng cho trang chủ sinh viên
     */
    List<SuKien> findByTrangThaiOrderByThoigianBatdauDesc(SuKien.TrangThaiSuKien trangThai);
}