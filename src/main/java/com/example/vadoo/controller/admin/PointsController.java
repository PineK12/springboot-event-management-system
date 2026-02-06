package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.admin.*;
import com.example.vadoo.entity.*;
import com.example.vadoo.repository.*;
import com.example.vadoo.service.admin.AdminExcelExportService;
import com.example.vadoo.service.admin.AdminPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/points")
@RequiredArgsConstructor
@Slf4j
public class PointsController {

    private final AdminPointsService adminPointsService;
    private final UserRepository userRepository;
    private final HocKyRepository hocKyRepository;
    private final DonViRepository donViRepository;
    private final SinhVienRepository sinhVienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final XepLoaiRepository xepLoaiRepository;
    private final HoTroHocBongRepository hoTroHocBongRepository;

    @Autowired
    private AdminExcelExportService adminExcelExportService;

    // ========== TRANG CHÍNH ==========

    @GetMapping
    public String pointsPage(
            @RequestParam(required = false) Integer hocKyId,
            @RequestParam(required = false) Integer khoaId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "list") String tab,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "0") Integer minDiem,
            Authentication authentication,
            Model model) {

        try {
            // User info
            String username = authentication.getName();
            User user = userRepository.findByUsernameWithDetails(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            model.addAttribute("user", user);
            model.addAttribute("displayName", user.getHoTen());

            // Học kỳ hiện tại
            HocKyDTO currentHocKy = adminPointsService.getCurrentHocKy();
            if (hocKyId == null && currentHocKy != null) {
                hocKyId = currentHocKy.getId();
            }

            // Đẩy danh sách Học kỳ và Khoa xuống để hiển thị trong Dropdown
            model.addAttribute("allHocKy", adminPointsService.getAllHocKy());
            model.addAttribute("allKhoa", adminPointsService.getAllKhoa());

            // TAB 1: Tra cứu & Chỉnh sửa
            if ("list".equals(tab)) {
                // A. Lấy danh sách Sinh viên theo bộ lọc
                List<SinhVien> sinhViens = sinhVienRepository.searchSinhVien(keyword, khoaId);

                List<XepLoai> xepLoaiConfigs = xepLoaiRepository.findAllByOrderByMinPointAsc();
                // B. Map sang DTO và TÍNH ĐIỂM CHUẨN (bao gồm điểm trừ)
                List<StudentPointDTO> dtos = sinhViens.stream().map(sv -> {

                    // --- LOGIC TÍNH ĐIỂM MỚI ---
                    int diemThucTe = calculateTotalRealPoints(sv.getUserId());
                    int soSuKienThamGia = dangKySuKienRepository.countEventsParticipated(sv.getUserId());
                    // ---------------------------

                    // Xác định xếp loại dựa trên điểm thực tế
                    String xepLoai = determineXepLoai(diemThucTe, xepLoaiConfigs);

                    return StudentPointDTO.builder()
                            .userId(sv.getUserId())
                            .mssv(sv.getMssv())
                            .ten(sv.getTen())
                            .khoaName(sv.getDonVi() != null ? sv.getDonVi().getTenDayDu() : "")
                            .soSuKien(soSuKienThamGia)
                            .diemTong(diemThucTe) // Hiển thị điểm đúng
                            .xepLoai(xepLoai)
                            .build();
                }).collect(Collectors.toList());

                model.addAttribute("students", dtos);

                // Dropdown filters
                model.addAttribute("allKhoa", donViRepository.findAllKhoa());
                model.addAttribute("allHocKy", hocKyRepository.findAll());
                model.addAttribute("selectedKhoaId", khoaId);
                model.addAttribute("selectedHocKyId", hocKyId);
                model.addAttribute("keyword", keyword);
            }

            // TAB 2: Cấu hình & Ngưỡng
            if ("config".equals(tab)) {
                List<XepLoaiDTO> xepLoais = adminPointsService.getXepLoaiByHocKy(hocKyId);
                List<HoTroConfigDTO> hoTros = adminPointsService.getHoTroConfigs();

                model.addAttribute("xepLoais", xepLoais);
                model.addAttribute("hoTros", hoTros);
            }

            // ================== TAB 3: XÉT DUYỆT & BÁO CÁO ==================
            if ("report".equals(tab)) {
                // 1. Xác định loại báo cáo
                String currentType = type != null ? type : "good";

                // 2. XÁC ĐỊNH ĐIỂM SÀN (NGƯỠNG) DỰA TRÊN CẤU HÌNH
                Integer currentMinDiem = minDiem;

                // Nếu người dùng chưa nhập điểm (minDiem = 0), ta lấy mặc định từ DB
                if (currentMinDiem <= 0) {
                    if ("good".equals(currentType)) {
                        // Tìm ngưỡng của loại "Tốt" trong cấu hình
                        // Nếu không tìm thấy (do admin đổi tên), fallback về 80
                        currentMinDiem = xepLoaiRepository.findByTenXepLoai("Tốt")
                                .map(XepLoai::getMinPoint)
                                .orElse(80);
                    } else {
                        // Tìm gói hỗ trợ có điểm thấp nhất
                        HoTroHocBong minSupport = hoTroHocBongRepository.findFirstByIsActiveTrueOrderByMinDrlRequiredAsc();
                        currentMinDiem = (minSupport != null) ? minSupport.getMinDrlRequired() : 85;
                    }
                }

                // 3. Lấy TOÀN BỘ sinh viên & TÍNH ĐIỂM THỰC TẾ
                List<SinhVien> allSv = sinhVienRepository.findAll();

                List<XepLoai> xepLoaiConfigs = xepLoaiRepository.findAllByOrderByMinPointAsc();
                // 4. TÍNH TOÁN & LỌC
                // (Cần biến final để dùng trong lambda)
                final int threshold = currentMinDiem;

                List<StudentPointDTO> filteredList = allSv.stream().map(sv -> {
                            // Gọi hàm tính điểm chuẩn (đã có ở dưới)
                            int realPoint = calculateTotalRealPoints(sv.getUserId());
                            String xepLoai = determineXepLoai(realPoint, xepLoaiConfigs);

                            return StudentPointDTO.builder()
                                    .userId(sv.getUserId())
                                    .mssv(sv.getMssv())
                                    .ten(sv.getTen())
                                    .khoaName(sv.getDonVi() != null ? sv.getDonVi().getTenDayDu() : "")
                                    .diemTong(realPoint)
                                    .xepLoai(xepLoai) // Hàm này cũng đã lấy config từ DB
                                    .build();
                        })
                        .filter(dto -> dto.getDiemTong() >= threshold) // Lọc theo ngưỡng lấy từ DB
                        .sorted((s1, s2) -> Integer.compare(s2.getDiemTong(), s1.getDiemTong())) // Sắp xếp giảm dần
                        .collect(Collectors.toList());

                model.addAttribute("reportStudents", filteredList);
                model.addAttribute("reportType", currentType);
                model.addAttribute("minDiem", currentMinDiem); // Truyền lại để hiển thị lên View
            }

            model.addAttribute("currentTab", tab);
            return "admin/points";

        } catch (Exception e) {
            log.error("Error loading points page", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "error";
        }
    }

    // --- HÀM TÍNH ĐIỂM CHUẨN (Giống bên StudentDashboardService) ---
    private int calculateTotalRealPoints(Integer studentId) {
        // Lấy toàn bộ lịch sử (Present + Absent)
        List<DangKySuKien> history = dangKySuKienRepository.findHistoryByStudentId(studentId);

        // Cộng dồn điểm (dương hoặc âm)
        int sum = history.stream().mapToInt(this::getPointFromRecord).sum();

        return Math.max(0, sum); // Không hiển thị số âm tổng (tùy nghiệp vụ)
    }

    private int getPointFromRecord(DangKySuKien d) {
        int diemGoc = d.getSuKien().getDiemCong();

        if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT) {
            // Có mặt: Lấy điểm đã lưu (ưu tiên) hoặc điểm gốc
            return d.getDiemNhanDuoc() != null ? d.getDiemNhanDuoc() : diemGoc;
        }
        else if (d.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.ABSENT) {
            // Vắng mặt: Trừ 50%
            return -(int) Math.ceil(diemGoc * 0.5);
        }

        return 0; // Chưa diễn ra hoặc đã hủy
    }

    /**
     * Hàm xác định xếp loại dựa trên điểm và danh sách cấu hình.
     * KHÔNG gọi DB trong hàm này để tối ưu tốc độ vòng lặp.
     */
    private String determineXepLoai(int score, List<XepLoai> configs) {
        if (configs == null || configs.isEmpty()) {
            return "Chưa cấu hình";
        }

        for (XepLoai xl : configs) {
            // Kiểm tra null an toàn cho min/max point
            int min = xl.getMinPoint() != null ? xl.getMinPoint() : 0;
            int max = xl.getMaxPoint() != null ? xl.getMaxPoint() : 100;

            if (score >= min && score <= max) {
                return xl.getTenXepLoai();
            }
        }
        return "Chưa xếp loại"; // Hoặc trả về loại thấp nhất tùy logic
    }

    // ========== API ENDPOINTS ==========

    // TAB 2: Lưu cấu hình xếp loại
    @PostMapping("/api/config/xeploai")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveXepLoaiConfig(@RequestBody List<XepLoaiDTO> configs) {
        try {
            adminPointsService.saveXepLoaiConfig(configs);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lưu cấu hình xếp loại thành công"
            ));
        } catch (Exception e) {
            log.error("Error saving xep loai config", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // TAB 2: Lưu cấu hình hỗ trợ
    @PostMapping("/api/config/hotro")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveHoTroConfig(@RequestBody List<HoTroConfigDTO> configs) { // Tên biến là 'configs'
        try {
            // SỬA DÒNG NÀY: Truyền biến 'configs' vào hàm xử lý List
            adminPointsService.saveHoTroConfigList(configs);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lưu cấu hình hỗ trợ thành công"
            ));
        } catch (Exception e) {
            log.error("Error saving ho tro config", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // TAB 3: Lấy báo cáo theo loại
    @GetMapping("/api/report")
    @ResponseBody
    public ResponseEntity<List<StudentPointDTO>> getReport(
            @RequestParam Integer hocKyId,
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "80") Integer minDiem) {

        try {
            List<StudentPointDTO> students;
            if ("good".equals(type)) {
                students = adminPointsService.getGoodStudents(hocKyId, minDiem);
            } else if ("support".equals(type)) {
                students = adminPointsService.getSupportEligibleStudents(hocKyId, minDiem);
            } else {
                students = adminPointsService.getAllStudentPoints(hocKyId, null, null);
            }

            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error getting report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // API XUẤT EXCEL (ĐÃ CẬP NHẬT LOGIC ĐỒNG BỘ)
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer hocKyId,
            @RequestParam(required = false) Integer khoaId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "0") Integer minDiem) {

        try {
            List<StudentPointDTO> dataToExport;
            String baseName;
            String title;

            // 1. XỬ LÝ LOGIC TAB 3 (XÉT DUYỆT & BÁO CÁO)
            if ("good".equals(type) || "support".equals(type)) {
                // A. Tự động lấy ngưỡng điểm nếu minDiem = 0
                int threshold = minDiem;
                if (threshold <= 0) {
                    if ("good".equals(type)) {
                        threshold = xepLoaiRepository.findByTenXepLoai("Tốt")
                                .map(XepLoai::getMinPoint).orElse(80);
                    } else {
                        HoTroHocBong minSupport = hoTroHocBongRepository.findFirstByIsActiveTrueOrderByMinDrlRequiredAsc();
                        threshold = (minSupport != null) ? minSupport.getMinDrlRequired() : 85;
                    }
                }

                // B. Lấy dữ liệu và tính điểm
                List<XepLoai> configs = xepLoaiRepository.findAllByOrderByMinPointAsc();
                final int finalThreshold = threshold;

                dataToExport = sinhVienRepository.findAll().stream()
                        .map(sv -> {
                            int realPoint = calculateTotalRealPoints(sv.getUserId());

                            // --- BỔ SUNG DÒNG NÀY ---
                            int eventCount = dangKySuKienRepository.countEventsParticipated(sv.getUserId());
                            // ------------------------

                            return StudentPointDTO.builder()
                                    .userId(sv.getUserId()) // Thêm ID cho chắc chắn
                                    .mssv(sv.getMssv())
                                    .ten(sv.getTen())
                                    .khoaName(sv.getDonVi() != null ? sv.getDonVi().getTenDayDu() : "")
                                    .diemTong(realPoint)
                                    .xepLoai(determineXepLoai(realPoint, configs))

                                    // --- QUAN TRỌNG: PHẢI SET SỐ SỰ KIỆN ---
                                    .soSuKien(eventCount)
                                    // ---------------------------------------

                                    .build();
                        })
                        .filter(dto -> dto.getDiemTong() >= finalThreshold)
                        .sorted((s1, s2) -> Integer.compare(s2.getDiemTong(), s1.getDiemTong()))
                        .collect(Collectors.toList());

                // C. Đặt tên file
                if ("good".equals(type)) {
                    baseName = "DS_SinhVienGioi";
                    title = "DANH SÁCH SINH VIÊN KHÁ/GIỎI (Điểm >= " + finalThreshold + ")";
                } else {
                    baseName = "DS_HoTro";
                    title = "DANH SÁCH SINH VIÊN NHẬN HỖ TRỢ (Điểm >= " + finalThreshold + ")";
                }
            }

            // 2. XỬ LÝ LOGIC TAB 1 (TỔNG HỢP / TRA CỨU)
            else {
                // Lấy danh sách theo bộ lọc tìm kiếm
                List<SinhVien> sinhViens = sinhVienRepository.searchSinhVien(keyword, khoaId);
                List<XepLoai> configs = xepLoaiRepository.findAllByOrderByMinPointAsc();

                dataToExport = sinhViens.stream().map(sv -> {
                    int realPoint = calculateTotalRealPoints(sv.getUserId());
                    return StudentPointDTO.builder()
                            .mssv(sv.getMssv()).ten(sv.getTen())
                            .khoaName(sv.getDonVi() != null ? sv.getDonVi().getTenDayDu() : "")
                            .soSuKien(dangKySuKienRepository.countEventsParticipated(sv.getUserId()))
                            .diemTong(realPoint)
                            .xepLoai(determineXepLoai(realPoint, configs))
                            .build();
                }).collect(Collectors.toList());

                baseName = "TongHop_DRL";
                title = "BẢNG TỔNG HỢP ĐIỂM RÈN LUYỆN";
            }

            // 3. SINH TÊN FILE & XUẤT
            String fileName = generateSmartFileName(baseName, hocKyId, khoaId);
            byte[] excelContent = adminExcelExportService.exportStudentPointsToExcel(dataToExport, title);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok().headers(headers).body(excelContent);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hàm sinh tên file thông minh: TênBase_HocKy_Khoa_Time.xlsx
     */
    private String generateSmartFileName(String baseName, Integer hocKyId, Integer khoaId) {
        StringBuilder sb = new StringBuilder(baseName);

        // Thêm thông tin Học kỳ
        if (hocKyId != null) {
            HocKy hk = hocKyRepository.findById(hocKyId).orElse(null);
            if (hk != null) {
                sb.append("_").append(toSlug(hk.getTenHocKy())); // VD: _HocKy1_2024
            }
        } else {
            sb.append("_TatCaHocKy");
        }

        // Thêm thông tin Khoa (nếu có)
        if (khoaId != null) {
            DonVi dv = donViRepository.findById(khoaId).orElse(null);
            if (dv != null) {
                sb.append("_").append(toSlug(dv.getTenDayDu())); // VD: _KhoaCNTT
            }
        }

        // Thêm Timestamp (yyMMdd_HHmm) để file không bị trùng
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HHmm"));
        sb.append("_").append(timeStamp);

        sb.append(".xlsx");
        return sb.toString();
    }

    /**
     * Hàm tiện ích: Chuyển tiếng Việt có dấu thành không dấu, bỏ ký tự đặc biệt
     * VD: "Khoa Công Nghệ Thông Tin" -> "KhoaCongNgheThongTin"
     */
    private String toSlug(String input) {
        if (input == null) return "";
        // 1. Chuẩn hóa unicode
        String temp = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        temp = pattern.matcher(temp).replaceAll("");

        // 2. Thay thế đ -> d
        temp = temp.replaceAll("đ", "d").replaceAll("Đ", "D");

        // 3. Giữ lại chỉ chữ và số, bỏ khoảng trắng
        return temp.replaceAll("[^a-zA-Z0-9]", "");
    }
}