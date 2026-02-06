package com.example.vadoo.controller.admin;

import com.example.vadoo.dto.DonViDTO;
import com.example.vadoo.entity.DonVi;
import com.example.vadoo.security.CustomUserDetails;
import com.example.vadoo.service.admin.AdminDonViService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class DonViController {

    private final AdminDonViService adminDonViService;

    /**
     * Trang danh sách đơn vị
     */
    @GetMapping("/donvi")
    public String listDonVi(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin {} accessing DonVi management", userDetails.getUsername());

        List<DonVi> donViList = adminDonViService.getAllDonVi();
        model.addAttribute("donViList", donViList);

        return "admin/list";
    }

    /**
     * Trang thêm đơn vị mới
     */
    @GetMapping("/donvi/create")
    public String showCreateForm(Model model) {
        model.addAttribute("donViDTO", new DonViDTO());
        model.addAttribute("possibleParents", adminDonViService.getPossibleParents());
        model.addAttribute("loaiDonViList", DonVi.LoaiDonVi.values());

        return "admin/form";
    }

    /**
     * Xử lý thêm đơn vị mới
     */
    @PostMapping("/donvi/create")
    public String createDonVi(@Valid @ModelAttribute DonViDTO donViDTO,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (result.hasErrors()) {
            model.addAttribute("possibleParents", adminDonViService.getPossibleParents());
            model.addAttribute("loaiDonViList", DonVi.LoaiDonVi.values());
            return "admin/form";
        }

        try {
            DonVi created = adminDonViService.createDonVi(donViDTO);
            log.info("Admin {} created DonVi: {}", userDetails.getUsername(), created.getTenDayDu());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo đơn vị thành công: " + created.getTenDayDu());
            return "redirect:/admin/donvi";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("possibleParents", adminDonViService.getPossibleParents());
            model.addAttribute("loaiDonViList", DonVi.LoaiDonVi.values());
            return "admin/form";
        }
    }

    /**
     * Trang sửa đơn vị
     */
    @GetMapping("/donvi/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        try {
            DonVi donVi = adminDonViService.getDonViById(id);

            DonViDTO dto = DonViDTO.builder()
                    .id(donVi.getId())
                    .loaiDonVi(donVi.getLoaiDonVi())
                    .tenDayDu(donVi.getTenDayDu())
                    .parentId(donVi.getParent() != null ? donVi.getParent().getId() : null)
                    .isActive(donVi.getIsActive())
                    .build();

            model.addAttribute("donViDTO", dto);
            model.addAttribute("possibleParents", adminDonViService.getPossibleParents());
            model.addAttribute("loaiDonViList", DonVi.LoaiDonVi.values());
            model.addAttribute("isEdit", true);

            return "admin/form";

        } catch (IllegalArgumentException e) {
            return "redirect:/admin/donvi";
        }
    }

    /**
     * Xử lý cập nhật đơn vị
     */
    @PostMapping("/donvi/edit/{id}")
    public String updateDonVi(@PathVariable Integer id,
                              @Valid @ModelAttribute DonViDTO donViDTO,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (result.hasErrors()) {
            model.addAttribute("possibleParents", adminDonViService.getPossibleParents());
            model.addAttribute("loaiDonViList", DonVi.LoaiDonVi.values());
            model.addAttribute("isEdit", true);
            return "admin/form";
        }

        try {
            DonVi updated = adminDonViService.updateDonVi(id, donViDTO);
            log.info("Admin {} updated DonVi ID {}: {}", userDetails.getUsername(), id, updated.getTenDayDu());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật đơn vị thành công: " + updated.getTenDayDu());
            return "redirect:/admin/donvi";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("possibleParents", adminDonViService.getPossibleParents());
            model.addAttribute("loaiDonViList", DonVi.LoaiDonVi.values());
            model.addAttribute("isEdit", true);
            return "admin/form";
        }
    }

    /**
     * Vô hiệu hóa đơn vị
     */
    @PostMapping("/donvi/deactivate/{id}")
    public String deactivateDonVi(@PathVariable Integer id,
                                  RedirectAttributes redirectAttributes,
                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            adminDonViService.deactivateDonVi(id);
            log.info("Admin {} deactivated DonVi ID: {}", userDetails.getUsername(), id);

            redirectAttributes.addFlashAttribute("successMessage", "Vô hiệu hóa đơn vị thành công");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/donvi";
    }

    /**
     * Kích hoạt lại đơn vị
     */
    @PostMapping("/donvi/activate/{id}")
    public String activateDonVi(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            adminDonViService.activateDonVi(id);
            log.info("Admin {} activated DonVi ID: {}", userDetails.getUsername(), id);

            redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt đơn vị thành công");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/donvi";
    }

    /**
     * Xóa hẳn đơn vị
     */
    @PostMapping("/donvi/delete/{id}")
    public String deleteDonVi(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            log.info("Request to delete DonVi ID: {}", id); // Thêm log này để biết request đã vào
            adminDonViService.deleteDonVi(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn đơn vị thành công.");
        } catch (DataIntegrityViolationException e) {
            log.warn("Cannot delete DonVi ID {} due to constraints. Switching to soft delete.", id);

            // Thử xóa mềm
            try {
                adminDonViService.deactivateDonVi(id);
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Không thể xóa vĩnh viễn vì có dữ liệu liên quan. Hệ thống đã chuyển sang trạng thái 'Vô hiệu hóa'.");
            } catch (Exception ex) {
                log.error("Error soft deleting DonVi ID {}", id, ex);
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi vô hiệu hóa: " + ex.getMessage());
            }
        } catch (Exception e) {
            // Bắt tất cả các lỗi khác và log ra
            log.error("Error deleting DonVi ID {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }

        return "redirect:/admin/donvi";
    }
}