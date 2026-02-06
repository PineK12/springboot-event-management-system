package com.example.vadoo.service.student;

import com.example.vadoo.dto.student.StudentEventDTO;
import com.example.vadoo.dto.student.StudentHistoryDTO;
import com.example.vadoo.dto.student.StudentPointStatsDTO;
import com.example.vadoo.dto.student.StudentProfileDTO;
import com.example.vadoo.entity.*;
import com.example.vadoo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentEventService {

    private final SuKienRepository suKienRepository;
    private final SinhVienRepository sinhVienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final XepLoaiRepository xepLoaiRepository;
    private final HoTroHocBongRepository hoTroHocBongRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy danh sách sự kiện hiển thị cho sinh viên (Kèm logic canCancel)
     */
    public List<StudentEventDTO> getEventsForStudent(Integer studentUserId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Lấy tất cả sự kiện đã duyệt và chưa kết thúc (hoặc lấy tất cả tùy nghiệp vụ)
        // Ở đây tôi lấy tất cả sự kiện APPROVED để sinh viên xem lịch sử
        List<SuKien> events = suKienRepository.findByTrangThaiOrderByThoigianBatdauDesc(SuKien.TrangThaiSuKien.APPROVED);

        // 2. Map sang DTO
        return events.stream().map(ev -> {
            // Kiểm tra sinh viên đã đăng ký sự kiện này chưa
            boolean isRegistered = dangKySuKienRepository.existsBySinhVienUserIdAndSuKienId(studentUserId, ev.getId());

            // --- LOGIC QUAN TRỌNG: Cờ canCancel ---
            // Điều kiện hủy:
            // 1. Phải đã đăng ký (isRegistered == true)
            // 2. Thời gian hiện tại phải TRƯỚC thời gian đóng đăng ký (hoặc trước khi sự kiện bắt đầu)
            boolean canCancel = isRegistered && now.isBefore(ev.getThoigianDongDangky());

            return StudentEventDTO.builder()
                    .id(ev.getId())
                    .tieuDe(ev.getTieuDe())
                    .moTa(ev.getMoTa())
                    .posterUrl(ev.getPosterUrl())
                    .thoigianBatdau(ev.getThoigianBatdau())
                    .thoigianKetthuc(ev.getThoigianKetthuc())
                    .thoigianDongDangky(ev.getThoigianDongDangky())
                    .diaDiem(ev.getDiaDiem())
                    .diemCong(ev.getDiemCong())
                    .registered(isRegistered) // Trạng thái đã đăng ký
                    .canCancel(canCancel)     // <--- Cờ quyết định hiển thị nút Hủy
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public String registerEvent(Integer userId, Integer eventId) {
        // 1. Tìm thông tin
        SinhVien sinhVien = sinhVienRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin sinh viên."));

        SuKien suKien = suKienRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại."));

        // 2. Kiểm tra trạng thái sự kiện
        if (suKien.getTrangThai() != SuKien.TrangThaiSuKien.APPROVED) {
            throw new RuntimeException("Sự kiện này chưa được duyệt hoặc đã bị hủy.");
        }

        // 3. Kiểm tra thời gian đăng ký
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(suKien.getThoigianMoDangky())) {
            throw new RuntimeException("Chưa đến thời gian mở cổng đăng ký.");
        }
        if (now.isAfter(suKien.getThoigianDongDangky())) {
            throw new RuntimeException("Đã hết hạn đăng ký tham gia.");
        }

        // 4. Kiểm tra đã đăng ký chưa
        if (dangKySuKienRepository.existsBySinhVienUserIdAndSuKienId(userId, eventId)) {
            throw new RuntimeException("Bạn đã đăng ký sự kiện này rồi!");
        }

        // 5. Kiểm tra giới hạn
        if (suKien.getGioiHan() > 0) {
            int daDangKy = dangKySuKienRepository.countBySuKienAndTrangThai(
                    suKien, DangKySuKien.TrangThaiDangKy.REGISTERED);
            if (daDangKy >= suKien.getGioiHan()) {
                throw new RuntimeException("Rất tiếc, sự kiện đã đủ số lượng đăng ký.");
            }
        }

        // 6. Tạo QR Code
        String qrCode = "EVT" + eventId + "-SV" + userId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 7. Lưu
        DangKySuKien dangKy = new DangKySuKien();
        dangKy.setSuKien(suKien);
        dangKy.setSinhVien(sinhVien);
        dangKy.setTrangThai(DangKySuKien.TrangThaiDangKy.REGISTERED);
        dangKy.setTrangthaiThamgia(DangKySuKien.TrangThaiThamGia.ABSENT);
        dangKy.setDiemNhanDuoc(0);
        dangKy.setQrRandomString(qrCode);

        dangKySuKienRepository.save(dangKy);

        return qrCode;
    }

    @Transactional
    public void cancelEventRegistration(Integer userId, Integer eventId) {
        // 1. Tìm bản ghi đăng ký
        DangKySuKien dangKy = dangKySuKienRepository.findBySinhVienUserIdAndSuKienId(userId, eventId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng ký sự kiện này."));

        // 2. --- BỔ SUNG VALIDATE ---
        // Chặn người dùng cố tình gọi API hủy khi đã hết hạn
        if (LocalDateTime.now().isAfter(dangKy.getSuKien().getThoigianDongDangky())) {
            throw new RuntimeException("Đã hết thời gian hủy đăng ký (Đã đóng sổ).");
        }

        // 3. Xóa bản ghi (Hủy đăng ký)
        dangKySuKienRepository.delete(dangKy);
    }

    // 1. Lấy lịch sử (Đã tích hợp bộ lọc và logic tính điểm chuẩn)
    public List<StudentHistoryDTO> getStudentHistory(Integer userId, String keyword, String semester, String status) {
        // 1. Lấy toàn bộ lịch sử thô từ DB
        List<DangKySuKien> allList = dangKySuKienRepository.findHistoryByStudentId(userId);

        // 2. Xử lý ngày tháng theo Học kỳ
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (semester != null && !semester.equals("ALL")) {
            try {
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
            } catch (Exception e) {
                // Phòng trường hợp format semester bị sai
                System.err.println("Lỗi parse học kỳ: " + semester);
            }
        }

        final LocalDateTime fStart = startDate;
        final LocalDateTime fEnd = endDate;

        return allList.stream()
                // --- FILTER 1: Keyword (Tên sự kiện) ---
                .filter(d -> {
                    if (keyword == null || keyword.isEmpty()) return true;
                    return d.getSuKien().getTieuDe().toLowerCase().contains(keyword.toLowerCase());
                })
                // --- FILTER 2: Status (Có mặt / Vắng) ---
                .filter(d -> {
                    if (status == null || status.equals("ALL")) return true;
                    return d.getTrangthaiThamgia().name().equalsIgnoreCase(status);
                })
                // --- FILTER 3: Thời gian (Học kỳ) ---
                .filter(d -> {
                    if (fStart == null || fEnd == null) return true;
                    LocalDateTime eventTime = d.getSuKien().getThoigianBatdau();
                    return eventTime.isAfter(fStart) && eventTime.isBefore(fEnd);
                })
                // --- MAP DTO ---
                .map(d -> {
                    // Lấy feedback
                    EventFeedback feedback = eventFeedbackRepository.findBySinhVienUserIdAndSuKienId(userId, d.getSuKien().getId()).orElse(null);

                    // Tạo tên viết tắt
                    String shortName = d.getSuKien().getTieuDe().substring(0, Math.min(2, d.getSuKien().getTieuDe().length())).toUpperCase();

                    // --- SỬ DỤNG HÀM HELPER ĐỂ TÍNH ĐIỂM (Thay thế đoạn if-else dài dòng cũ) ---
                    int finalPoint = calculateRealPoints(d);
                    // ------------------------------------------------------------------------

                    return StudentHistoryDTO.builder()
                            .id(d.getId())
                            .eventTitle(d.getSuKien().getTieuDe())
                            .donViName(d.getSuKien().getDonVi().getTenDayDu())
                            .eventShortName(shortName)
                            .eventTime(d.getSuKien().getThoigianBatdau())
                            .diaDiem(d.getSuKien().getDiaDiem())
                            .attendanceStatus(d.getTrangthaiThamgia().name())
                            .diemCong(finalPoint) // Gán điểm đã tính toán (có thể là số âm)
                            .userRating(feedback != null ? feedback.getRating() : 0)
                            .userComment(feedback != null ? feedback.getComment() : "")

                            // --- MAP DỮ LIỆU REPLY ---
                            .replyContent(feedback != null ? feedback.getReplyContent() : null)
                            .replyTime(feedback != null ? feedback.getThoigianReply() : null)
                            .repliedByName(feedback != null && feedback.getRepliedBy() != null
                                    ? feedback.getRepliedBy().getHoTen() : "")
                            // -------------------------

                            .build();
                }).collect(Collectors.toList());
    }

    // 2. Lấy thống kê (Đã sửa logic tính tổng)
    public Map<String, Object> getStudentStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();

        // Lấy toàn bộ lịch sử (Chỉ Present/Absent)
        List<DangKySuKien> allHistory = dangKySuKienRepository.findHistoryByStudentId(userId);

        // --- LOGIC MỚI: Tính tổng bằng Java để bao gồm điểm âm ---
        int totalPoints = allHistory.stream()
                .mapToInt(this::calculateRealPoints) // Gọi hàm helper
                .sum();
        // --------------------------------------------------------

        // Đếm số lần tham gia
        long eventsJoined = allHistory.stream()
                .filter(d -> d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT)
                .count();

        // Tính tỷ lệ
        int totalRecorded = allHistory.size();
        int attendanceRate = totalRecorded > 0 ? (int)((eventsJoined * 100) / totalRecorded) : 0;

        stats.put("totalPoints", totalPoints);
        stats.put("eventsJoined", eventsJoined);
        stats.put("attendanceRate", attendanceRate);

        return stats;
    }

    // 3. Gửi đánh giá (Đã sửa: Chặn sửa đánh giá)
    public void submitRating(Integer userId, Long registrationId, Integer rating, String comment) {
        DangKySuKien dk = dangKySuKienRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đăng ký."));

        // Kiểm tra xem đã đánh giá chưa
        Optional<EventFeedback> existingFeedback = eventFeedbackRepository
                .findBySinhVienUserIdAndSuKienId(userId, dk.getSuKien().getId());

        // --- LOGIC MỚI: Nếu đã có thì chặn luôn ---
        if (existingFeedback.isPresent()) {
            throw new RuntimeException("Bạn đã đánh giá sự kiện này rồi, không thể chỉnh sửa!");
        }
        // ------------------------------------------

        // Nếu chưa có thì tạo mới
        EventFeedback feedback = new EventFeedback();
        feedback.setSinhVien(dk.getSinhVien());
        feedback.setSuKien(dk.getSuKien());
        feedback.setRating(rating);
        feedback.setComment(comment);

        eventFeedbackRepository.save(feedback);
    }

    /**
     * Hàm helper: Tính điểm thực tế (Dùng chung cho cả List và Stats)
     */
    private int calculateRealPoints(DangKySuKien d) {
        int diemGoc = d.getSuKien().getDiemCong();

        if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT) {
            // Có mặt: Lấy điểm đã lưu hoặc điểm gốc
            return d.getDiemNhanDuoc() != null ? d.getDiemNhanDuoc() : diemGoc;
        }
        else if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.ABSENT) {
            // Vắng mặt: Trừ 50% làm tròn lên => Ra số âm
            int diemTru = (int) Math.ceil(diemGoc * 0.5);
            return -diemTru;
        }

        return 0; // Các trạng thái khác (nếu có)
    }

    public StudentPointStatsDTO getPointStats(Integer userId, String semester) {
        // 1. Tính tổng điểm thực tế (Logic cũ)
        List<StudentHistoryDTO> history = getStudentHistory(userId, null, semester, "ALL");
        int rawPoints = history.stream().mapToInt(StudentHistoryDTO::getDiemCong).sum();

        // Giới hạn 0-100 (Nếu quy chế cho phép >100 thì bỏ dòng này)
        final int totalPoints = Math.max(0, Math.min(100, rawPoints));

        // 2. --- LOGIC XẾP LOẠI (DYNAMIC) ---
        XepLoai currentRank = xepLoaiRepository.findByPoint(totalPoints)
                .orElse(XepLoai.builder()
                        .id(0)
                        .tenXepLoai("Chưa xếp loại")
                        .minPoint(0)
                        .maxPoint(0)
                        .build());

        String rankName = currentRank.getTenXepLoai();
        String rankColor = getRankColor(totalPoints); // Hàm phụ để lấy màu cho đẹp
        String rankDesc = "Điểm hiện tại: " + totalPoints + "/" + currentRank.getMaxPoint();

        // 3. --- LOGIC MỤC TIÊU TIẾP THEO ---
        XepLoai nextRank = xepLoaiRepository.findNextRank(totalPoints).orElse(null);
        int nextRankThreshold = (nextRank != null) ? nextRank.getMinPoint() : 100;
        int pointsToNextRank = (nextRank != null) ? (nextRank.getMinPoint() - totalPoints) : 0;

        // 4. --- LOGIC HỌC BỔNG (DYNAMIC) ---
        // Lấy tiêu chuẩn học bổng cơ bản nhất từ DB
        HoTroHocBong scholarship = hoTroHocBongRepository.findBaseScholarship()
                .orElse(HoTroHocBong.builder()
                        .id(0)
                        .tenHoTro("Học bổng KKHT")
                        .minDrlRequired(85)
                        .isActive(true)
                        .build()); // Default nếu DB trống

        int scholarshipThreshold = scholarship.getMinDrlRequired();
        boolean isEligible = totalPoints >= scholarshipThreshold;
        int pointsNeeded = isEligible ? 0 : (scholarshipThreshold - totalPoints);

        return StudentPointStatsDTO.builder()
                .totalPoints(totalPoints)
                .rankName(rankName)
                .rankColor(rankColor)
                .rankDescription(rankDesc)
                .isScholarshipEligible(isEligible)
                .scholarshipThreshold(scholarshipThreshold)
                .pointsNeededForScholarship(pointsNeeded)
                .nextRankThreshold(nextRankThreshold)
                .pointsToNextRank(pointsToNextRank)
                .build();
    }

    // Hàm phụ: Chọn màu sắc dựa trên điểm số (Để giao diện vẫn đẹp)
    // Cái này có thể lưu trong DB bảng xep_loai (cột color_code) nếu muốn dynamic 100%
    private String getRankColor(int points) {
        if (points >= 90) return "text-purple-600";
        if (points >= 80) return "text-green-600";
        if (points >= 65) return "text-blue-600";
        if (points >= 50) return "text-orange-500";
        return "text-red-500";
    }

    // 1. Lấy thông tin Profile
    public StudentProfileDTO getProfile(Integer userId) {
        SinhVien sv = sinhVienRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));

        return StudentProfileDTO.builder()
                .mssv(sv.getMssv())
                .hoTen(sv.getUser().getHoTen())
                .lop(sv.getTenLop())
                .khoa(sv.getDonVi() != null ? sv.getDonVi().getTenDayDu() : "Chưa cập nhật")
                .email(sv.getUser().getEmail())
                .sdt(sv.getUser().getSdt())
                .avatarUrl(sv.getUser().getAvatarUrl())
                // Giả sử SinhVien entity có thêm các trường này, nếu chưa có thì bỏ qua hoặc thêm vào Entity
                .ngaySinh(sv.getNgaySinh())
                .build();
    }

    // 2. Cập nhật thông tin (Có validate SĐT)
    @Transactional
    public void updateProfile(Integer userId, StudentProfileDTO dto) {
        SinhVien sv = sinhVienRepository.findById(userId).orElseThrow();

        // Validate SDT
        if (dto.getSdt() == null || dto.getSdt().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }
        if (!dto.getSdt().matches("\\d{10,11}")) {
            throw new RuntimeException("Số điện thoại không hợp lệ (Phải là 10-11 chữ số)!");
        }

        // Cập nhật User Info

        // Cập nhật User Info
        // 1. Cập nhật tên vào bảng SinhVien (vì User chỉ chứa tài khoản login)
        sv.setTen(dto.getHoTen());

        // 2. Các thông tin khác vẫn nằm ở User
        sv.getUser().setSdt(dto.getSdt());
        sv.getUser().setEmail(dto.getEmail());
        sv.getUser().setAvatarUrl(dto.getAvatarUrl());

        sv.getUser().setSdt(dto.getSdt());
        sv.getUser().setEmail(dto.getEmail());
        sv.getUser().setAvatarUrl(dto.getAvatarUrl());

        // Cập nhật SinhVien Info (Nếu có trường này trong Entity)
        // sv.setNgaySinh(dto.getNgaySinh());
        // sv.setDiaChi(dto.getDiaChi());

        sinhVienRepository.save(sv);
    }

    // 3. Đổi mật khẩu
    @Transactional
    public void changePassword(Integer userId, String currentPass, String newPass, String confirmPass) {
        SinhVien sv = sinhVienRepository.findById(userId).orElseThrow();
        String currentHash = sv.getUser().getPassword();

        // 1. Check mật khẩu cũ
        if (!passwordEncoder.matches(currentPass, currentHash)) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng!");
        }
        // 2. Check xác nhận mật khẩu mới
        if (!newPass.equals(confirmPass)) {
            throw new RuntimeException("Xác nhận mật khẩu mới không khớp!");
        }
        // 3. Check độ mạnh (Tùy chọn)
        if (newPass.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }

        // 4. Lưu
        sv.getUser().setPassword(passwordEncoder.encode(newPass));
        sinhVienRepository.save(sv);
    }

    /**
     * Hàm lấy Entity SinhVien đầy đủ
     * Dùng để cập nhật lại thông tin trong Session sau khi user sửa profile
     */
    public SinhVien getSinhVienById(Integer userId) {
        // Giả định: ID của SinhVien trùng với ID của User (hoặc tìm theo User ID)
        // Nếu logic của bạn khác, hãy đổi thành sinhVienRepository.findByUserId(userId)
        return sinhVienRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin sinh viên với ID: " + userId));
    }
}