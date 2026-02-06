package com.example.vadoo.repository;

import com.example.vadoo.entity.DangKySuKien;
import com.example.vadoo.entity.SinhVien;
import com.example.vadoo.entity.SuKien;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DangKySuKienRepository extends JpaRepository<DangKySuKien, Long> {

    // Tìm đăng ký của sinh viên cho sự kiện
    Optional<DangKySuKien> findBySuKienAndSinhVien(SuKien suKien, SinhVien sinhVien);

    // Đếm số sự kiện sinh viên đã tham gia (có điểm danh)
    @Query("SELECT COUNT(d) FROM DangKySuKien d " +
            "WHERE d.sinhVien = :sinhVien " +
            "AND d.thoigianCheckin IS NOT NULL " +
            "AND d.trangthaiThamgia = 'PRESENT'")
    long countAttendedEventsBySinhVien(@Param("sinhVien") SinhVien sinhVien);

    // Tính tổng điểm của sinh viên
    @Query("SELECT COALESCE(SUM(d.diemNhanDuoc), 0) FROM DangKySuKien d " +
            "WHERE d.sinhVien = :sinhVien " +
            "AND d.thoigianCheckin IS NOT NULL " +
            "AND d.trangthaiThamgia = 'PRESENT'")
    int sumPointsBySinhVien(@Param("sinhVien") SinhVien sinhVien);

    @Query("SELECT COUNT(d) FROM DangKySuKien d " +
            "JOIN d.suKien s " +
            "WHERE (d.thoigianDangky BETWEEN :startDate AND :endDate) " +
            "AND s.trangThai IN ('APPROVED', 'COMPLETED')")
    long countValidRegistrationsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Tính tổng điểm rèn luyện đã được cộng (chỉ tính sinh viên đã tham gia)
    @Query("SELECT COALESCE(SUM(d.diemNhanDuoc), 0) FROM DangKySuKien d " +
            "WHERE d.trangthaiThamgia = 'PRESENT'")
    long calculateTotalPoints();

    long countBySuKien(SuKien suKien);

    // ✅ THÊM ĐOẠN NÀY ĐỂ HỖ TRỢ XÓA SỰ KIỆN
    @Transactional
    @Modifying
    @Query("DELETE FROM DangKySuKien d WHERE d.suKien.id = :suKienId")
    void deleteAllBySuKienId(@Param("suKienId") Integer suKienId);

    // Đếm tổng đăng ký
    @Query("SELECT COUNT(d) FROM DangKySuKien d WHERE d.trangThai = 'REGISTERED'")
    long countAllRegistrations();

    // Đếm đã điểm danh
    @Query("SELECT COUNT(d) FROM DangKySuKien d WHERE d.thoigianCheckin IS NOT NULL")
    long countAttendedRegistrations();

    // Đếm đăng ký theo khoảng thời gian
    @Query("SELECT COUNT(d) FROM DangKySuKien d WHERE d.thoigianDangky BETWEEN :startDate AND :endDate")
    long countRegistrationsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Đếm đã điểm danh theo sự kiện
    @Query("SELECT COUNT(d) FROM DangKySuKien d WHERE d.suKien = :suKien AND d.thoigianCheckin IS NOT NULL")
    long countAttendedBySuKien(@Param("suKien") SuKien suKien);

    // Lấy danh sách đăng ký kèm sinh viên
    @Query("SELECT d FROM DangKySuKien d " +
            "LEFT JOIN FETCH d.sinhVien sv " +
            "LEFT JOIN FETCH sv.user " +
            "WHERE d.suKien.id = :suKienId " +
            "ORDER BY d.thoigianDangky DESC")
    List<DangKySuKien> findBySuKienIdWithSinhVien(@Param("suKienId") Integer suKienId);

    // Đếm đăng ký mới theo BTC và khoảng thời gian
    @Query("SELECT COUNT(dk) FROM DangKySuKien dk " +
            "JOIN dk.suKien s " +
            "WHERE s.btc.id = :btcId " +
            "AND dk.thoigianDangky BETWEEN :startDate AND :endDate")
    Integer countNewRegistrationsByBtcAndDateRange(
            @Param("btcId") Integer btcId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Đếm đăng ký theo sự kiện và trạng thái
    @Query("SELECT COUNT(dk) FROM DangKySuKien dk " +
            "WHERE dk.suKien = :suKien AND dk.trangThai = :trangThai")
    int countBySuKienAndTrangThai(SuKien suKien, DangKySuKien.TrangThaiDangKy trangThai);

    // Thêm hàm này: Đếm số người đã Check-in (có thời gian check-in khác null)
    int countBySuKienAndThoigianCheckinNotNull(SuKien suKien);

    // Tìm các đăng ký của SV mà sự kiện chưa diễn ra
    @Query("SELECT d FROM DangKySuKien d " +
            "WHERE d.sinhVien.userId = :studentId " + // Chú ý: .userId (khớp với Entity SinhVien)
            "AND d.suKien.thoigianBatdau > :now")
    List<DangKySuKien> findRegisteredUpcomingEvents(@Param("studentId") Integer studentId,
                                                    @Param("now") LocalDateTime now);

    // 1. Tính TỔNG điểm rèn luyện tích lũy của sinh viên
    // (COALESCE để nếu null thì trả về 0)
    @Query("SELECT COALESCE(SUM(d.diemNhanDuoc), 0) FROM DangKySuKien d " +
            "WHERE d.sinhVien.userId = :studentId")
    Integer sumTotalPoints(@Param("studentId") Integer studentId);

    // 2. Tính điểm cộng TRONG THÁNG này (Dựa vào ngày bắt đầu sự kiện)
    @Query("SELECT COALESCE(SUM(d.diemNhanDuoc), 0) FROM DangKySuKien d " +
            "WHERE d.sinhVien.userId = :studentId " +
            "AND d.suKien.thoigianBatdau BETWEEN :start AND :end")
    Integer sumPointsInMonth(@Param("studentId") Integer studentId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @Query("SELECT d.suKien.id FROM DangKySuKien d WHERE d.sinhVien.userId = :studentId")
    List<Integer> findEventIdsByStudentId(@Param("studentId") Integer studentId);

    /**
     * Lấy lịch sử đăng ký của sinh viên, sắp xếp ngày đăng ký giảm dần (Mới nhất lên đầu)
     */
    @Query("SELECT d FROM DangKySuKien d " +
            "WHERE d.sinhVien.userId = :studentId " +
            "ORDER BY d.thoigianDangky DESC")
    List<DangKySuKien> findBySinhVienUserIdOrderByThoigianDangkyDesc(@Param("studentId") Integer studentId);

    // Kiểm tra sinh viên đã đăng ký sự kiện này chưa (trả về true/false)
    boolean existsBySinhVienUserIdAndSuKienId(Integer userId, Integer eventId);

    /**
     * Tìm bản ghi đăng ký cụ thể của 1 sinh viên cho 1 sự kiện
     * Dùng Optional để xử lý null an toàn
     */
    @Query("SELECT d FROM DangKySuKien d " +
            "WHERE d.sinhVien.userId = :userId " +
            "AND d.suKien.id = :eventId")
    Optional<DangKySuKien> findBySinhVienUserIdAndSuKienId(@Param("userId") Integer userId,
                                                           @Param("eventId") Integer eventId);

    Optional<DangKySuKien> findByQrRandomString(String qrRandomString);

    /**
     * Lấy danh sách đăng ký theo ID sự kiện
     * (Hàm này được dùng trong BtcDashboardService -> getRegistrations)
     */
    List<DangKySuKien> findBySuKienId(Integer suKienId);

    /**
     * Lấy lịch sử điểm danh (Bao gồm cả người đi và người vắng)
     * Điều kiện: Trạng thái KHÁC 'REGISTERED' (tức là đã có kết quả rồi)
     */
    @Query("SELECT d FROM DangKySuKien d " +
            "WHERE d.suKien.btc.id = :btcId " +
            "AND d.trangthaiThamgia IN ('PRESENT', 'ABSENT') " +
            "ORDER BY d.id DESC") // Sắp xếp tạm theo ID
    List<DangKySuKien> findPointHistoryByBtc(@Param("btcId") Integer btcId);

    /**
     * Lấy lịch sử điểm danh có lọc theo thời gian
     * Nếu startDate/endDate là null thì coi như không lọc (lấy hết)
     */
    @Query("SELECT d FROM DangKySuKien d " +
            "WHERE d.suKien.btc.id = :btcId " +
            // QUAN TRỌNG: Phải có cả 'ABSENT' ở đây
            "AND d.trangthaiThamgia IN ('PRESENT', 'ABSENT') " +
            "AND (:startDate IS NULL OR d.thoigianCheckin >= :startDate) " +
            "AND (:endDate IS NULL OR d.thoigianCheckin <= :endDate) " +
            "ORDER BY d.thoigianCheckin DESC")
    List<DangKySuKien> findPointHistoryByBtcAndDate(@Param("btcId") Integer btcId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy lịch sử tham gia của sinh viên (Sắp xếp mới nhất)
     * Chỉ lấy những sự kiện đã kết thúc (hoặc trạng thái đã checkin/vắng)
     */
    @Query("SELECT d FROM DangKySuKien d " +
            "WHERE d.sinhVien.user.id = :userId " +
            "AND d.trangthaiThamgia IN ('PRESENT', 'ABSENT') " +
            "ORDER BY d.suKien.thoigianBatdau DESC")
    List<DangKySuKien> findHistoryByStudentId(@Param("userId") Integer userId);

    // Đếm tổng số sự kiện đã tham gia (PRESENT)
    @Query("SELECT COUNT(d) FROM DangKySuKien d WHERE d.sinhVien.user.id = :userId AND d.trangthaiThamgia = 'PRESENT'")
    int countEventsParticipated(@Param("userId") Integer userId);

    // Tính tổng điểm tích lũy
    @Query("SELECT SUM(d.diemNhanDuoc) FROM DangKySuKien d WHERE d.sinhVien.user.id = :userId")
    Integer sumPointsByStudent(@Param("userId") Integer userId);

    // Đếm số đăng ký mới của BTC kể từ ngày X (ví dụ: 7 ngày trước)
    @Query("SELECT COUNT(d) FROM DangKySuKien d " +
            "WHERE d.suKien.btc.id = :btcId " +
            "AND d.thoigianDangky >= :startDate")
    Integer countNewRegistrationsByBtc(@Param("btcId") Integer btcId,
                                       @Param("startDate") LocalDateTime startDate);

    // --- BỔ SUNG HÀM NÀY ĐỂ FIX LỖI ---
    // Đếm số lượng theo trạng thái tham gia (để đếm số người PRESENT)
    long countBySuKienAndTrangthaiThamgia(SuKien suKien, DangKySuKien.TrangThaiThamGia trangthaiThamgia);
}