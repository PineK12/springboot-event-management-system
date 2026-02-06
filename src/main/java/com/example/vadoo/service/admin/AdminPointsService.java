package com.example.vadoo.service.admin;

import com.example.vadoo.aop.LogActivity;
import com.example.vadoo.dto.admin.*;
import com.example.vadoo.entity.*;
import com.example.vadoo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPointsService {

    private final SinhVienRepository sinhVienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final SuKienRepository suKienRepository;
    private final HocKyRepository hocKyRepository;
    private final XepLoaiRepository xepLoaiRepository;
    private final HoTroHocBongRepository hoTroHocBongRepository;
    private final DonViRepository donViRepository;

    // ========== TAB 1: TRA CỨU & CHỈNH SỬA ==========

    /**
     * Lấy danh sách sinh viên với điểm DRL
     */
    public List<StudentPointDTO> getAllStudentPoints(Integer hocKyId, Integer khoaId, String keyword) {

        // 1. Lọc ngay tại Database thay vì lọc trên RAM
        List<SinhVien> students = sinhVienRepository.searchStudents(keyword, khoaId);

        // 2. Lấy học kỳ
        HocKy hocKy = hocKyId != null
                ? hocKyRepository.findById(hocKyId).orElse(null)
                : hocKyRepository.findByIsCurrent(true).orElse(null);

        if (hocKy == null) {
            return new ArrayList<>();
        }

        // 3. Convert sang DTO (Giữ nguyên logic tính điểm)
        return students.stream()
                .map(sv -> calculateStudentPoint(sv, hocKy))
                .sorted((a, b) -> Integer.compare(b.getDiemTong(), a.getDiemTong()))
                .collect(Collectors.toList());
    }

    /**
     * Tính điểm DRL cho 1 sinh viên trong 1 học kỳ
     */
    private StudentPointDTO calculateStudentPoint(SinhVien sinhVien, HocKy hocKy) {
        // Lấy tất cả sự kiện trong học kỳ
        List<SuKien> eventsInSemester = suKienRepository.findAll().stream()
                .filter(sk -> isEventInSemester(sk, hocKy))
                .collect(Collectors.toList());

        // Lấy danh sách đăng ký của sinh viên
        int soSuKien = 0;
        int diemTong = 0;

        for (SuKien suKien : eventsInSemester) {
            Optional<DangKySuKien> dangKyOpt = dangKySuKienRepository
                    .findBySuKienAndSinhVien(suKien, sinhVien);

            if (dangKyOpt.isPresent()) {
                DangKySuKien dangKy = dangKyOpt.get();

                // Chỉ tính nếu đã điểm danh
                if (dangKy.getThoigianCheckin() != null &&
                        dangKy.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT) {
                    soSuKien++;
                    diemTong += dangKy.getDiemNhanDuoc();
                }
            }
        }

        // Xác định xếp loại
        String xepLoai = determineXepLoai(diemTong, hocKy.getId());

        return StudentPointDTO.builder()
                .userId(sinhVien.getUser().getId())
                .mssv(sinhVien.getMssv())
                .ten(sinhVien.getTen())
                .tenLop(sinhVien.getTenLop())
                .khoaName(sinhVien.getDonVi() != null ? sinhVien.getDonVi().getTenDayDu() : "N/A")
                .soSuKien(soSuKien)
                .diemTong(diemTong)
                .xepLoai(xepLoai)
                .hocKyId(hocKy.getId())
                .build();
    }

    /**
     * Kiểm tra sự kiện có trong học kỳ không
     */
    private boolean isEventInSemester(SuKien suKien, HocKy hocKy) {
        LocalDate eventDate = suKien.getThoigianBatdau().toLocalDate();
        return !eventDate.isBefore(hocKy.getStartDate()) &&
                !eventDate.isAfter(hocKy.getEndDate());
    }

    /**
     * Xác định xếp loại dựa vào điểm
     */
    private String determineXepLoai(Integer diem, Integer hocKyId) {
        Optional<XepLoai> xepLoaiOpt = xepLoaiRepository.findByHocKyIdAndDiem(hocKyId, diem);
        return xepLoaiOpt.map(XepLoai::getTenXepLoai).orElse("Chưa xếp loại");
    }

    // ========== TAB 2: CẤU HÌNH & NGƯỠNG ==========

    /**
     * Lấy danh sách xếp loại theo học kỳ
     */
    public List<XepLoaiDTO> getXepLoaiByHocKy(Integer hocKyId) {
        List<XepLoai> xepLoais = hocKyId != null
                ? xepLoaiRepository.findByHocKyIdOrderByMinPointDesc(hocKyId)
                : xepLoaiRepository.findAll();

        return xepLoais.stream()
                .map(this::convertToXepLoaiDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lưu/Cập nhật cấu hình xếp loại
     */
    @Transactional
    @LogActivity(action = "UPDATE_CONFIG", description = "Cập nhật cấu hình xếp loại", targetTable = "xep_loai")
    public void saveXepLoaiConfig(List<XepLoaiDTO> configs) {
        for (XepLoaiDTO dto : configs) {
            XepLoai xepLoai = null;

            if (dto.getId() != null) {
                xepLoai = xepLoaiRepository.findById(dto.getId()).orElse(null);
            }

            // Nếu không tìm thấy hoặc ID null -> Tạo mới
            if (xepLoai == null) {
                xepLoai = new XepLoai();
                // Gán tên nếu tạo mới
                xepLoai.setTenXepLoai(dto.getTenXepLoai());
            }

            // Cập nhật giá trị
            xepLoai.setMinPoint(dto.getMinPoint());
            xepLoai.setMaxPoint(dto.getMaxPoint());

            // Gán học kỳ (quan trọng)
            if (dto.getHocKyId() != null) {
                HocKy hocKy = hocKyRepository.findById(dto.getHocKyId())
                        .orElseThrow(() -> new RuntimeException("Học kỳ không tồn tại"));
                xepLoai.setHocKy(hocKy);
            } else {
                // Nếu không gửi học kỳ lên, lấy học kỳ hiện tại làm mặc định
                HocKy current = hocKyRepository.findByIsCurrent(true).orElse(null);
                if (current != null) xepLoai.setHocKy(current);
            }

            xepLoaiRepository.save(xepLoai);
        }
    }

    /**
     * Lấy cấu hình hỗ trợ
     */
    public List<HoTroConfigDTO> getHoTroConfigs() {
        return hoTroHocBongRepository.findAllActiveOrderByMinDrlDesc().stream()
                .map(this::convertToHoTroDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lưu danh sách cấu hình hỗ trợ (MỚI - Cần thêm hàm này)
     */
    @Transactional
    @LogActivity(action = "UPDATE_CONFIG", description = "Cập nhật danh sách hỗ trợ", targetTable = "hotro_hocbong")
    public void saveHoTroConfigList(List<HoTroConfigDTO> configs) {
        for (HoTroConfigDTO dto : configs) {
            if (dto.getId() != null) {
                HoTroHocBong hoTro = hoTroHocBongRepository.findById(dto.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy hỗ trợ ID: " + dto.getId()));

                // Cập nhật điểm yêu cầu
                if (dto.getMinDrlRequired() != null) {
                    hoTro.setMinDrlRequired(dto.getMinDrlRequired());
                }

                // Cập nhật tên/tiền nếu cần (tùy logic Frontend gửi lên)
                if (dto.getTenHoTro() != null) hoTro.setTenHoTro(dto.getTenHoTro());
                if (dto.getSoTien() != null) hoTro.setSoTien(dto.getSoTien());
                if (dto.getIsActive() != null) hoTro.setIsActive(dto.getIsActive());

                hoTroHocBongRepository.save(hoTro);
            }
        }
    }

    /**
     * Lưu/Cập nhật cấu hình hỗ trợ
     */
    @Transactional
    @LogActivity(action = "UPDATE_CONFIG", description = "Cập nhật cấu hình hỗ trợ", targetTable = "hotro_hocbong")
    public void saveHoTroConfig(HoTroConfigDTO dto) {
        HoTroHocBong hoTro = dto.getId() != null
                ? hoTroHocBongRepository.findById(dto.getId())
                .orElse(new HoTroHocBong())
                : new HoTroHocBong();

        hoTro.setTenHoTro(dto.getTenHoTro());
        hoTro.setMinDrlRequired(dto.getMinDrlRequired());
        hoTro.setMoTa(dto.getMoTa());
        hoTro.setSoTien(dto.getSoTien());
        hoTro.setIsActive(dto.getIsActive());

        hoTroHocBongRepository.save(hoTro);
    }

    // ========== TAB 3: XÉT DUYỆT & BÁO CÁO ==========

    /**
     * Lấy danh sách sinh viên đạt loại Tốt trở lên
     */
    public List<StudentPointDTO> getGoodStudents(Integer hocKyId, Integer minDiem) {
        List<StudentPointDTO> allStudents = getAllStudentPoints(hocKyId, null, null);

        return allStudents.stream()
                .filter(s -> s.getDiemTong() >= minDiem)
                .filter(s -> s.getXepLoai().equals("Xuất sắc") || s.getXepLoai().equals("Tốt"))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách sinh viên đủ điều kiện hỗ trợ
     */
    public List<StudentPointDTO> getSupportEligibleStudents(Integer hocKyId, Integer minDrlForSupport) {
        List<StudentPointDTO> allStudents = getAllStudentPoints(hocKyId, null, null);

        return allStudents.stream()
                .filter(s -> s.getDiemTong() >= minDrlForSupport)
                .collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    /**
     * Lấy danh sách học kỳ
     */
    public List<HocKyDTO> getAllHocKy() {
        return hocKyRepository.findAllOrderByStartDateDesc().stream()
                .map(this::convertToHocKyDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy học kỳ hiện tại
     */
    public HocKyDTO getCurrentHocKy() {
        return hocKyRepository.findByIsCurrent(true)
                .map(this::convertToHocKyDTO)
                .orElse(null);
    }

    /**
     * Lấy danh sách khoa
     */
    public List<DonVi> getAllKhoa() {
        return donViRepository.findByLoaiDonVi(DonVi.LoaiDonVi.KHOA);
    }

    /**
     * Lấy danh sách sinh viên có điểm >= minPoint
     * (Hàm chung dùng cho cả export Excel và xem báo cáo)
     */
    public List<StudentPointDTO> getStudentsByMinPoint(Integer minPoint) {
        // Lấy học kỳ hiện tại (mặc định)
        HocKy currentHocKy = hocKyRepository.findByIsCurrent(true).orElse(null);
        Integer hocKyId = currentHocKy != null ? currentHocKy.getId() : null;

        // Lấy tất cả và lọc
        List<StudentPointDTO> allStudents = getAllStudentPoints(hocKyId, null, null);

        if (minPoint == null || minPoint <= 0) {
            return allStudents;
        }

        return allStudents.stream()
                .filter(s -> s.getDiemTong() >= minPoint)
                .collect(Collectors.toList());
    }

    // ========== CONVERTERS ==========

    private XepLoaiDTO convertToXepLoaiDTO(XepLoai xepLoai) {
        return XepLoaiDTO.builder()
                .id(xepLoai.getId())
                .tenXepLoai(xepLoai.getTenXepLoai())
                .minPoint(xepLoai.getMinPoint())
                .maxPoint(xepLoai.getMaxPoint())
                .hocKyId(xepLoai.getHocKy() != null ? xepLoai.getHocKy().getId() : null)
                .build();
    }

    private HocKyDTO convertToHocKyDTO(HocKy hocKy) {
        return HocKyDTO.builder()
                .id(hocKy.getId())
                .tenHocKy(hocKy.getTenHocKy())
                .startDate(hocKy.getStartDate())
                .endDate(hocKy.getEndDate())
                .isCurrent(hocKy.getIsCurrent())
                .build();
    }

    private HoTroConfigDTO convertToHoTroDTO(HoTroHocBong hoTro) {
        return HoTroConfigDTO.builder()
                .id(hoTro.getId())
                .tenHoTro(hoTro.getTenHoTro())
                .minDrlRequired(hoTro.getMinDrlRequired())
                .moTa(hoTro.getMoTa())
                .soTien(hoTro.getSoTien())
                .isActive(hoTro.getIsActive())
                .build();
    }
}