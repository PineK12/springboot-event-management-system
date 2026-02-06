package com.example.vadoo.service.admin;

import com.example.vadoo.aop.LogActivity;
import com.example.vadoo.dto.DonViDTO;
import com.example.vadoo.entity.DonVi;
import com.example.vadoo.repository.DonViRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDonViService {

    private final DonViRepository donViRepository;

    /**
     * Lấy tất cả đơn vị
     */
    public List<DonVi> getAllDonVi() {
        return donViRepository.findAllOrdered();
    }

    /**
     * Lấy đơn vị theo ID
     */
    public DonVi getDonViById(Integer id) {
        return donViRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị với ID: " + id));
    }

    /**
     * Tạo đơn vị mới
     */
    @Transactional
    @LogActivity(action = "CREATE_DONVI", description = "Tạo đơn vị mới", targetTable = "don_vi")
    public DonVi createDonVi(DonViDTO dto) {
        log.info("Creating new DonVi: {}", dto.getTenDayDu());

        // Kiểm tra trùng tên
        if (donViRepository.existsByTenDayDu(dto.getTenDayDu())) {
            throw new IllegalArgumentException("Tên đơn vị đã tồn tại: " + dto.getTenDayDu());
        }

        DonVi donVi = DonVi.builder()
                .loaiDonVi(dto.getLoaiDonVi())
                .tenDayDu(dto.getTenDayDu())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        // Nếu có parent
        if (dto.getParentId() != null) {
            DonVi parent = getDonViById(dto.getParentId());
            donVi.setParent(parent);
        }

        DonVi saved = donViRepository.save(donVi);
        log.info("Created DonVi with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Cập nhật đơn vị
     */
    @Transactional
    @LogActivity(action = "UPDATE_DONVI", description = "Cập nhật thông tin đơn vị", targetTable = "don_vi")
    public DonVi updateDonVi(Integer id, DonViDTO dto) {
        log.info("Updating DonVi ID: {}", id);

        DonVi donVi = getDonViById(id);

        // Kiểm tra trùng tên (trừ chính nó)
        if (donViRepository.existsByTenDayDuAndIdNot(dto.getTenDayDu(), id)) {
            throw new IllegalArgumentException("Tên đơn vị đã tồn tại: " + dto.getTenDayDu());
        }

        donVi.setLoaiDonVi(dto.getLoaiDonVi());
        donVi.setTenDayDu(dto.getTenDayDu());
        donVi.setIsActive(dto.getIsActive());

        // Cập nhật parent
        if (dto.getParentId() != null) {
            DonVi parent = getDonViById(dto.getParentId());
            // Không cho phép đơn vị là parent của chính nó
            if (parent.getId().equals(id)) {
                throw new IllegalArgumentException("Đơn vị không thể là đơn vị cha của chính nó");
            }
            donVi.setParent(parent);
        } else {
            donVi.setParent(null);
        }

        DonVi updated = donViRepository.save(donVi);
        log.info("Updated DonVi ID: {}", id);
        return updated;
    }

    /**
     * Xóa mềm đơn vị (vô hiệu hóa)
     */
    @Transactional
    @LogActivity(action = "DEACTIVATE_DONVI", description = "Vô hiệu hóa đơn vị", targetTable = "don_vi")
    public void deactivateDonVi(Integer id) {
        log.info("Deactivating DonVi ID: {}", id);

        DonVi donVi = getDonViById(id);
        donVi.setIsActive(false);
        donViRepository.save(donVi);

        log.info("Deactivated DonVi ID: {}", id);
    }

    /**
     * Kích hoạt lại đơn vị
     */
    @Transactional
    @LogActivity(action = "ACTIVATE_DONVI", description = "Kích hoạt lại đơn vị", targetTable = "don_vi")
    public void activateDonVi(Integer id) {
        log.info("Activating DonVi ID: {}", id);

        DonVi donVi = getDonViById(id);
        donVi.setIsActive(true);
        donViRepository.save(donVi);

        log.info("Activated DonVi ID: {}", id);
    }

    /**
     * Xóa hẳn đơn vị (cẩn thận!)
     */
    @Transactional
    @LogActivity(action = "DELETE_DONVI", description = "Xóa vĩnh viễn đơn vị", targetTable = "don_vi")
    public void deleteDonVi(Integer id) {
        log.warn("Permanently deleting DonVi ID: {}", id);

        DonVi donVi = getDonViById(id);

        // Kiểm tra xem có đơn vị con không
        List<DonVi> children = donViRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa đơn vị có đơn vị con. Hãy xóa các đơn vị con trước.");
        }

        donViRepository.delete(donVi);
        log.warn("Permanently deleted DonVi ID: {}", id);
    }

    /**
     * Lấy danh sách đơn vị có thể làm parent
     */
    public List<DonVi> getPossibleParents() {
        return donViRepository.findByIsActiveTrue();
    }
}