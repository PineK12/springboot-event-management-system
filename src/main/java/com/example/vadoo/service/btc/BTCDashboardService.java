package com.example.vadoo.service.btc;

import com.example.vadoo.dto.admin.DashboardStatsDTO; // Tạm dùng DTO chung hoặc tạo DTO riêng
import com.example.vadoo.dto.btc.BTCEventSummaryDTO;
import com.example.vadoo.dto.btc.FeedbackDTO;
import com.example.vadoo.dto.btc.PointHistoryDTO;
import com.example.vadoo.dto.btc.RegistrationDTO;
import com.example.vadoo.entity.*;
import com.example.vadoo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BTCDashboardService {

    private final SuKienRepository suKienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final BtcRepository btcRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> getDashboardStats(Integer btcUserId) {
        Map<String, Object> stats = new HashMap<>();

        Btc btc = btcRepository.findById(btcUserId).orElse(null);
        if (btc == null) return stats;

        LocalDateTime now = LocalDateTime.now();

        // 1. Event Stats
        int eventsRunning = suKienRepository.countByBtcIdAndTrangThaiAndThoigianBatdauBeforeAndThoigianKetthucAfter(
                btcUserId, SuKien.TrangThaiSuKien.APPROVED, now, now);

        int eventsPending = suKienRepository.countByBtcIdAndTrangThai(
                btcUserId, SuKien.TrangThaiSuKien.PENDING);

        // 2. New Registrations (7 ngày qua)
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        Integer newRegistrations = dangKySuKienRepository.countNewRegistrationsByBtc(btcUserId, sevenDaysAgo);

        // 3. Rating
        Double rawRating = eventFeedbackRepository.getAverageRatingByBtcId(btcUserId);
        double avgRating = (rawRating != null) ? Math.round(rawRating * 10.0) / 10.0 : 0.0;

        // --- PUT DỮ LIỆU VÀO MAP ---
        stats.put("eventRunning", eventsRunning);
        stats.put("eventPending", eventsPending);

        // SỬA DÒNG NÀY: Đổi key thành "newRegistrations" cho khớp HTML
        stats.put("newRegistrations", newRegistrations);

        stats.put("btcName", btc.getTen());
        stats.put("donViName", btc.getDonVi().getTenDayDu());
        stats.put("avgRating", avgRating);

        return stats;
    }

    /**
     * Lấy danh sách sự kiện sắp diễn ra + đang chạy
     */
    public List<BTCEventSummaryDTO> getUpcomingEvents(Integer btcUserId, int limit) {
        LocalDateTime now = LocalDateTime.now();

        // Gọi hàm repository bạn vừa tạo ở bước trước
        List<SuKien> events = suKienRepository.findUpcomingEventsByBtc(btcUserId, now);

        return events.stream()
                .limit(limit) // Giới hạn số lượng (ví dụ 5 sự kiện)
                .map(event -> {
                    // 1. Đếm số lượng đăng ký (LƯU Ý: Dùng Enum trực tiếp để tránh lỗi)
                    int currentReg = dangKySuKienRepository.countBySuKienAndTrangThai(
                            event, DangKySuKien.TrangThaiDangKy.REGISTERED);

                    // 2. Tính phần trăm lấp đầy
                    int percent = event.getGioiHan() > 0
                            ? (currentReg * 100 / event.getGioiHan())
                            : 0;

                    // 3. Xác định sự kiện có đang chạy không (để hiện chấm xanh/đỏ ở giao diện)
                    boolean isRunning = event.getThoigianBatdau().isBefore(now)
                            && event.getThoigianKetthuc().isAfter(now);

                    // 4. Map sang DTO
                    return BTCEventSummaryDTO.builder()
                            .eventId(event.getId())
                            .tieuDe(event.getTieuDe())
                            .thoigianBatdau(event.getThoigianBatdau())
                            .thoigianKetthuc(event.getThoigianKetthuc())
                            .currentRegistrations(currentReg)
                            .gioiHan(event.getGioiHan())
                            .trangThai(String.valueOf(event.getTrangThai())) // Enum sang String để hiển thị
                            .registrationPercent(percent)
                            .isRunning(isRunning)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<RegistrationDTO> getRegistrations(Integer eventId, String keyword, String status) {
        List<DangKySuKien> list;

        // 1. Lấy danh sách thô từ DB
        if (eventId != null) {
            list = dangKySuKienRepository.findBySuKienId(eventId);
        } else {
            return new ArrayList<>();
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. Stream Filter & Map
        return list.stream()
                .filter(d -> {
                    // A. Filter Keyword
                    if (keyword != null && !keyword.isEmpty()) {
                        String k = keyword.toLowerCase();
                        boolean matchName = d.getSinhVien().getTen().toLowerCase().contains(k);
                        boolean matchMssv = d.getSinhVien().getMssv().toLowerCase().contains(k);
                        if (!matchName && !matchMssv) return false;
                    }

                    // B. Filter Status (LOGIC MỚI - KHỚP VỚI HIỂN THỊ)
                    if (status != null && !status.equals("ALL")) {
                        boolean isEnded = now.isAfter(d.getSuKien().getThoigianKetthuc());
                        boolean isPresent = d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT;
                        boolean isCanceled = d.getTrangThai() == DangKySuKien.TrangThaiDangKy.CANCELLED_BY_SYSTEM
                                || d.getTrangThai() == DangKySuKien.TrangThaiDangKy.CANCELLED_BY_USER;

                        // Lọc người CÓ MẶT
                        if (status.equals("PRESENT")) {
                            return isPresent;
                        }
                        // Lọc người VẮNG (Bao gồm: Đã set ABSENT hoặc Đã qua giờ mà chưa checkin)
                        else if (status.equals("ABSENT")) {
                            // Không đi + (Đã set vắng HOẶC Đã hết giờ) + Không phải Hủy
                            return !isPresent && !isCanceled &&
                                    (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.ABSENT || isEnded);
                        }
                        // Lọc người MỚI ĐĂNG KÝ (Chưa diễn ra)
                        else if (status.equals("REGISTERED")) {
                            // Chưa checkin + Chưa hết giờ + Không phải Hủy
                            return !isPresent && !isCanceled && !isEnded;
                        }
                    }
                    return true;
                })
                .map(d -> {
                    // C. Logic Map trạng thái hiển thị (Giống code bạn đã có)
                    String displayStatus = "REGISTERED";
                    LocalDateTime eventEndTime = d.getSuKien().getThoigianKetthuc();

                    if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT) {
                        displayStatus = "PRESENT";
                    } else if (d.getTrangThai() == DangKySuKien.TrangThaiDangKy.CANCELLED_BY_SYSTEM
                            || d.getTrangThai() == DangKySuKien.TrangThaiDangKy.CANCELLED_BY_USER) {
                        displayStatus = "CANCELED";
                    } else {
                        // Logic tự động chuyển Vắng
                        if (now.isAfter(eventEndTime)) {
                            displayStatus = "ABSENT";
                        } else {
                            displayStatus = "REGISTERED";
                        }
                    }

                    return RegistrationDTO.builder()
                            .id(d.getId())
                            .mssv(d.getSinhVien().getMssv())
                            .hoTen(d.getSinhVien().getTen())
                            .lop(d.getSinhVien().getTenLop())
                            .thoiGianDangKy(d.getThoigianDangky())
                            .trangThai(displayStatus)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<PointHistoryDTO> getPointHistory(Integer btcId, String keyword, String semester) {
        // 1. Lấy toàn bộ dữ liệu (Cả Present và Absent)
        List<DangKySuKien> allList = dangKySuKienRepository.findPointHistoryByBtc(btcId);

        // 2. Tính toán ngày bắt đầu/kết thúc
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (semester != null && !semester.equals("ALL")) {
            int year = Integer.parseInt(semester.split("-")[0]);
            int ky = Integer.parseInt(semester.split("-")[1]);
            if (ky == 1) {
                startDate = LocalDateTime.of(year, 9, 1, 0, 0);
                endDate = LocalDateTime.of(year + 1, 1, 31, 23, 59);
            } else if (ky == 2) {
                startDate = LocalDateTime.of(year, 2, 1, 0, 0);
                endDate = LocalDateTime.of(year, 6, 30, 23, 59);
            } else {
                startDate = LocalDateTime.of(year, 7, 1, 0, 0);
                endDate = LocalDateTime.of(year, 8, 31, 23, 59);
            }
        }

        final LocalDateTime fStart = startDate;
        final LocalDateTime fEnd = endDate;

        // 3. Stream Filter & Map
        return allList.stream()
                // A. Filter Keyword
                .filter(d -> {
                    if (keyword == null || keyword.isEmpty()) return true;
                    String k = keyword.toLowerCase();
                    return d.getSinhVien().getTen().toLowerCase().contains(k) ||
                            d.getSinhVien().getMssv().toLowerCase().contains(k) ||
                            d.getSuKien().getTieuDe().toLowerCase().contains(k);
                })
                // B. Filter Ngày tháng (Xử lý thông minh cho người Vắng)
                .filter(d -> {
                    if (fStart == null || fEnd == null) return true;

                    // Nếu có checkin -> Lấy time checkin
                    // Nếu vắng (null) -> Lấy time kết thúc sự kiện
                    LocalDateTime timePoint = d.getThoigianCheckin();
                    if (timePoint == null) {
                        timePoint = d.getSuKien().getThoigianKetthuc();
                    }

                    return timePoint.isAfter(fStart) && timePoint.isBefore(fEnd);
                })
                // C. Map to DTO
                .map(d -> {
                    Integer finalPoint = 0;
                    int diemGoc = d.getSuKien().getDiemCong();

                    // Logic tính điểm
                    if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT) {
                        finalPoint = d.getDiemNhanDuoc() != null ? d.getDiemNhanDuoc() : diemGoc;
                    } else if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.ABSENT) {
                        int diemTru = (int) Math.ceil(diemGoc * 0.5);
                        finalPoint = -diemTru;
                    }

                    // Logic time hiển thị
                    LocalDateTime displayTime = d.getThoigianCheckin();
                    if (displayTime == null) displayTime = d.getSuKien().getThoigianKetthuc();

                    return PointHistoryDTO.builder()
                            .mssv(d.getSinhVien().getMssv())
                            .hoTen(d.getSinhVien().getTen())
                            .tenLop(d.getSinhVien().getTenLop())
                            .tenSuKien(d.getSuKien().getTieuDe())
                            .thoiGianCheckIn(displayTime)
                            .diem(finalPoint)
                            .trangThai(d.getTrangthaiThamgia().name())
                            .build();
                })
                // D. Sort (Mới nhất lên đầu)
                .sorted((o1, o2) -> o2.getThoiGianCheckIn().compareTo(o1.getThoiGianCheckIn()))
                .collect(Collectors.toList());
    }

    // Hàm tính tổng điểm BTC đã cấp
    public int getTotalPointsGiven(Integer btcId) {
        // CŨ (Gây lỗi): getPointHistory(btcId, null)

        // MỚI (Sửa lại): Truyền thêm "ALL" vào tham số semester
        return getPointHistory(btcId, null, "ALL")
                .stream()
                .mapToInt(PointHistoryDTO::getDiem)
                .sum();
    }

    // 1. Lấy danh sách Feedback (Đã cập nhật theo Entity của bạn)
    public List<FeedbackDTO> getFeedbacks(Integer btcId, Integer eventId) {
        List<EventFeedback> list;

        // 1. Logic lấy dữ liệu từ DB
        if (eventId != null) {
            list = eventFeedbackRepository.findByEventId(eventId);
        } else {
            list = eventFeedbackRepository.findAllByBtcId(btcId); // Đảm bảo Repo có hàm này
        }

        // 2. Map sang DTO
        return list.stream().map(f -> {
            // Xử lý an toàn cho avatar (tránh lỗi nếu tên null)
            String name = f.getSinhVien().getTen();
            String avatarChar = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";

            // Xử lý an toàn cho tên người trả lời
            String replierName = (f.getRepliedBy() != null) ? f.getRepliedBy().getHoTen() : "";

            return FeedbackDTO.builder()
                    .id(f.getId()) // <--- QUAN TRỌNG: Phải có ID để form Reply hoạt động
                    .studentName(name)
                    .studentAvatarChar(avatarChar)
                    .eventName(f.getSuKien().getTieuDe())
                    .rating(f.getRating())
                    .comment(f.getComment())
                    .createdAt(f.getThoigianTao())

                    // --- MAP ĐÚNG VỚI FEEDBACK DTO ---
                    .replyContent(f.getReplyContent())
                    .replyTime(f.getThoigianReply())      // Tên biến trong DTO là replyTime
                    .repliedByName(replierName)           // Tên biến trong DTO là repliedByName
                    // ---------------------------------

                    .build();
        }).collect(Collectors.toList());
    }

    // 2. Tính điểm trung bình (Giữ nguyên)
    public double getAverageRating(Integer btcId) {
        Double avg = eventFeedbackRepository.getAverageRatingByBtc(btcId);
        if (avg == null) return 0.0;
        return Math.round(avg * 10.0) / 10.0;
    }

    // 1. Cập nhật thông tin cá nhân
    public void updateProfile(Integer userId, String sdt, String avatarUrl) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setSdt(sdt);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
    }

    // 2. Đổi mật khẩu
    public boolean changePassword(Integer userId, String oldPass, String newPass) {
        User user = userRepository.findById(userId).orElseThrow();

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPass, user.getPassword())) {
            return false; // Sai mật khẩu cũ
        }

        // Mã hóa và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
        return true;
    }

    // 1. Cập nhật hàm mapToDTO (trong logic lấy danh sách)
    private FeedbackDTO mapToDTO(EventFeedback f) {
        String name = f.getSinhVien().getTen();
        // Lấy chữ cái đầu (VD: "Nam" -> "N")
        String charAvatar = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";

        return FeedbackDTO.builder()
                .id(f.getId())
                .studentName(name)
                .studentAvatarChar(charAvatar)
                .eventName(f.getSuKien().getTieuDe())
                .rating(f.getRating())
                .comment(f.getComment())
                .createdAt(f.getThoigianTao())

                // --- MAP REPLY ---
                .replyContent(f.getReplyContent())
                .replyTime(f.getThoigianReply())
                // ----------------
                .build();
    }

    // 2. Thêm hàm Xử lý Reply
    public void replyToFeedback(Long feedbackId, String content, Integer btcUserId) {
        EventFeedback feedback = eventFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá này!"));

        // Lấy thông tin user BTC đang trả lời
        User btcUser = userRepository.findById(btcUserId).orElseThrow();

        feedback.setReplyContent(content);
        feedback.setRepliedBy(btcUser); // Lưu ai là người trả lời
        feedback.setThoigianReply(LocalDateTime.now());

        eventFeedbackRepository.save(feedback);
    }

}