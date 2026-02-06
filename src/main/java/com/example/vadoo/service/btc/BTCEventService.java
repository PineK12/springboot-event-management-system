package com.example.vadoo.service.btc;

import com.example.vadoo.dto.btc.BTCEventCreateDTO;
import com.example.vadoo.dto.btc.BTCEventDetailDTO;
import com.example.vadoo.entity.DangKySuKien;
import com.example.vadoo.entity.DonVi;
import com.example.vadoo.entity.SuKien;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.DangKySuKienRepository;
import com.example.vadoo.repository.SuKienRepository;
import com.example.vadoo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BTCEventService {

    private final SuKienRepository suKienRepository;
    private final DangKySuKienRepository dangKySuKienRepository;
    private final UserRepository userRepository;

    public BTCEventDetailDTO getEventDetail(Integer eventId, Integer btcId) {
        // 1. Tìm sự kiện (có check quyền sở hữu)
        SuKien sk = suKienRepository.findByIdAndBtcId(eventId, btcId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại hoặc bạn không có quyền truy cập"));

        // 2. Lấy số liệu thống kê
        int countReg = dangKySuKienRepository.countBySuKienAndTrangThai(
                sk, DangKySuKien.TrangThaiDangKy.REGISTERED);

        int countCheckin = dangKySuKienRepository.countBySuKienAndThoigianCheckinNotNull(sk);

        // 3. Logic hiển thị
        LocalDateTime now = LocalDateTime.now();
        boolean isRunning = sk.getThoigianBatdau().isBefore(now) && sk.getThoigianKetthuc().isAfter(now);
        boolean isEditable = sk.getTrangThai() == SuKien.TrangThaiSuKien.PENDING; // Chỉ cho sửa khi đang chờ duyệt (hoặc tùy logic bạn)

        // 4. Map sang DTO
        return BTCEventDetailDTO.builder()
                .id(sk.getId())
                .tieuDe(sk.getTieuDe())
                .moTa(sk.getMoTa())
                .noiDung(sk.getNoiDung())
                .posterUrl(sk.getPosterUrl())
                .diaDiem(sk.getDiaDiem())
                .thoigianBatdau(sk.getThoigianBatdau())
                .thoigianKetthuc(sk.getThoigianKetthuc())
                .thoigianMoDangky(sk.getThoigianMoDangky())
                .thoigianDongDangky(sk.getThoigianDongDangky())
                .gioiHan(sk.getGioiHan())
                .diemCong(sk.getDiemCong())
                .soLuongDangKy(countReg)
                .soLuongCheckIn(countCheckin)
                .trangThai(sk.getTrangThai().name())
                .liDoTuChoi(sk.getLiDo())
                .isRunning(isRunning)
                .isEditable(isEditable)
                .build();
    }

    @Transactional
    public void createEvent(BTCEventCreateDTO dto, Integer btcUserId) {
        // 1. VALIDATION LOGIC NÂNG CAO (Nghiệp vụ)
        if (dto.getThoigianKetthuc().isBefore(dto.getThoigianBatdau())) {
            throw new IllegalArgumentException("Thời gian Kết thúc phải sau thời gian Bắt đầu");
        }
        if (dto.getThoigianDongDangky().isBefore(dto.getThoigianMoDangky())) {
            throw new IllegalArgumentException("Thời gian Đóng đăng ký phải sau khi Mở");
        }
        // Quy tắc quan trọng: Phải chốt sổ trước khi sự kiện diễn ra
        if (dto.getThoigianDongDangky().isAfter(dto.getThoigianBatdau())) {
            throw new IllegalArgumentException("Phải chốt danh sách đăng ký trước khi sự kiện bắt đầu");
        }

        // 2. Lấy User & Đơn vị
        User btcUser = userRepository.findById(btcUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User BTC"));

        // Giả sử User có quan hệ OneToOne với Btc, và Btc có quan hệ với DonVi
        if (btcUser.getBtc() == null) {
            throw new RuntimeException("Tài khoản này chưa có hồ sơ BTC");
        }
        DonVi donVi = btcUser.getBtc().getDonVi();

        // 3. Map DTO -> Entity
        SuKien sukien = new SuKien();
        sukien.setTieuDe(dto.getTieuDe());
        sukien.setDiaDiem(dto.getDiaDiem());
        sukien.setMoTa(dto.getMoTa());
        sukien.setPosterUrl(dto.getPosterUrl()); // Lưu link ảnh

        sukien.setThoigianBatdau(dto.getThoigianBatdau());
        sukien.setThoigianKetthuc(dto.getThoigianKetthuc());
        sukien.setThoigianMoDangky(dto.getThoigianMoDangky());
        sukien.setThoigianDongDangky(dto.getThoigianDongDangky());

        sukien.setGioiHan(dto.getGioiHan());
        sukien.setDiemCong(dto.getDiemCong());
        sukien.setDiemThoiThieu(dto.getDiemThoiThieu());

        // 4. Gán quan hệ & Trạng thái
        sukien.setBtc(btcUser);
        sukien.setDonVi(donVi);
        sukien.setTrangThai(SuKien.TrangThaiSuKien.PENDING); // Mặc định chờ duyệt

        // 5. Lưu xuống DB
        suKienRepository.save(sukien);
    }

    @Transactional
    public void updateEvent(Integer eventId, BTCEventCreateDTO dto, Integer btcUserId) {
        // 1. Tìm sự kiện của đúng BTC này
        SuKien sukien = suKienRepository.findByIdAndBtcId(eventId, btcUserId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại hoặc bạn không có quyền sửa"));

        // 2. CHECK QUYỀN: Chỉ cho sửa khi chưa được duyệt
        if (sukien.getTrangThai() == SuKien.TrangThaiSuKien.APPROVED) {
            throw new RuntimeException("Không thể sửa sự kiện đã được duyệt! Vui lòng liên hệ Admin.");
        }

        // 3. Cập nhật thông tin (Giữ nguyên ID, Người tạo, Ngày tạo)
        sukien.setTieuDe(dto.getTieuDe());
        sukien.setDiaDiem(dto.getDiaDiem());
        sukien.setMoTa(dto.getMoTa());
        sukien.setPosterUrl(dto.getPosterUrl());

        sukien.setThoigianBatdau(dto.getThoigianBatdau());
        sukien.setThoigianKetthuc(dto.getThoigianKetthuc());
        sukien.setThoigianMoDangky(dto.getThoigianMoDangky());
        sukien.setThoigianDongDangky(dto.getThoigianDongDangky());

        sukien.setGioiHan(dto.getGioiHan());
        sukien.setDiemCong(dto.getDiemCong());
        sukien.setDiemThoiThieu(dto.getDiemThoiThieu());

        // 4. Nếu đang bị Từ chối -> Chuyển thành Chờ duyệt để Admin xem lại
        if (sukien.getTrangThai() == SuKien.TrangThaiSuKien.REJECTED) {
            sukien.setTrangThai(SuKien.TrangThaiSuKien.PENDING);
        }

        suKienRepository.save(sukien);
    }

    // Thêm hàm lấy chi tiết cho API (để điền vào Modal)
    public BTCEventCreateDTO getEventForEdit(Integer eventId) {
        SuKien sk = suKienRepository.findById(eventId).orElseThrow();
        // Map Entity -> DTO (Bạn có thể dùng Mapper cho nhanh)
        BTCEventCreateDTO dto = new BTCEventCreateDTO();
        dto.setTieuDe(sk.getTieuDe());
        dto.setDiaDiem(sk.getDiaDiem());
        dto.setMoTa(sk.getMoTa());
        dto.setPosterUrl(sk.getPosterUrl());
        dto.setThoigianBatdau(sk.getThoigianBatdau());
        dto.setThoigianKetthuc(sk.getThoigianKetthuc());
        dto.setThoigianMoDangky(sk.getThoigianMoDangky());
        dto.setThoigianDongDangky(sk.getThoigianDongDangky());
        dto.setGioiHan(sk.getGioiHan());
        dto.setDiemCong(sk.getDiemCong());
        dto.setDiemThoiThieu(sk.getDiemThoiThieu());
        return dto;
    }

    @Transactional
    public void deleteEvent(Integer eventId, Integer btcUserId) {
        // 1. Tìm sự kiện (Check quyền sở hữu)
        SuKien sukien = suKienRepository.findByIdAndBtcId(eventId, btcUserId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại hoặc bạn không có quyền xóa"));

        // 2. Chỉ cho xóa khi chưa được duyệt (PENDING hoặc REJECTED)
        // Nếu đã APPROVED mà muốn xóa thì phải xin Admin hủy
        if (sukien.getTrangThai() == SuKien.TrangThaiSuKien.APPROVED) {
            throw new RuntimeException("Không thể xóa sự kiện đã được duyệt! Vui lòng liên hệ Admin để hủy.");
        }

        // 3. Xóa dữ liệu liên quan (nếu có)
        // Nếu có sinh viên đăng ký rồi thì xóa luôn record đăng ký của họ
        // (Hoặc nếu muốn soft-delete thì set is_active=false, nhưng ở đây ta làm hard-delete cho đơn giản)
        dangKySuKienRepository.deleteAll(sukien.getDangKySuKiens());

        // 4. Xóa sự kiện
        suKienRepository.delete(sukien);
    }

    @Transactional
    public Map<String, Object> processCheckIn(String qrCode, Integer currentEventId) {
        Map<String, Object> result = new HashMap<>();

        // 1. Tìm vé
        Optional<DangKySuKien> opt = dangKySuKienRepository.findByQrRandomString(qrCode);
        if (opt.isEmpty()) {
            result.put("status", "error");
            result.put("message", "Mã vé không tồn tại!");
            return result;
        }

        DangKySuKien ve = opt.get();

        // 2. Check đúng sự kiện không
        if (!ve.getSuKien().getId().equals(currentEventId)) {
            result.put("status", "error");
            result.put("message", "Vé sai sự kiện (Vé của: " + ve.getSuKien().getTieuDe() + ")");
            return result;
        }

        // 3. Check đã điểm danh chưa
        if (ve.getTrangthaiThamgia() == DangKySuKien.TrangThaiThamGia.PRESENT) {
            result.put("status", "warning"); // Cảnh báo nhẹ
            result.put("message", "Sinh viên này đã điểm danh rồi!");
            result.put("studentName", ve.getSinhVien().getTen());
            result.put("mssv", ve.getSinhVien().getMssv());
            return result;
        }

        // 4. OK -> Cập nhật
        ve.setTrangthaiThamgia(DangKySuKien.TrangThaiThamGia.PRESENT);
        ve.setThoigianCheckin(LocalDateTime.now());
        ve.setDiemNhanDuoc(ve.getSuKien().getDiemCong());
        dangKySuKienRepository.save(ve);

        result.put("status", "success");
        result.put("message", "Hợp lệ");
        result.put("studentName", ve.getSinhVien().getTen());
        result.put("mssv", ve.getSinhVien().getMssv());

        return result;
    }
}