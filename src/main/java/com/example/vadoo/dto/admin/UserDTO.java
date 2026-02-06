package com.example.vadoo.dto.admin;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Integer id;

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username chỉ được chứa chữ cái, số và dấu gạch dưới")
    private String username;

    @Size(min = 6, max = 100, message = "Mật khẩu phải có 6-100 ký tự")
    private String password;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String email;

    @Pattern(regexp = "^$|^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String sdt;

    @NotNull(message = "Vui lòng chọn vai trò")
    private Integer roleId;

    private String roleName;

    private String avatarUrl;
    private Boolean isActive;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2-100 ký tự")
    private String ten;

    @Size(max = 100, message = "Chức vụ không được vượt quá 100 ký tự")
    private String chucVu;

    @Pattern(regexp = "^$|^[0-9A-Za-z]{6,15}$", message = "MSSV phải có 6-15 ký tự")
    private String mssv;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate ngaySinh;

    @Pattern(regexp = "^$|^(NAM|NU|KHAC)$", message = "Giới tính không hợp lệ")
    private String gioiTinh;

    @Size(max = 50, message = "Tên lớp không được vượt quá 50 ký tự")
    private String tenLop;

    @Size(max = 100, message = "Chuyên ngành không được vượt quá 100 ký tự")
    private String chuyenNganh;

    private Integer donViId;
    private String donViName;

    private String thoiGianTao;
    private String thoiGianCapNhat;

    public void validateForRole() {
        if (roleName == null) {
            throw new IllegalArgumentException("Role name không được null");
        }

        if ("BTC".equalsIgnoreCase(roleName)) {
            if (donViId == null) {
                throw new IllegalArgumentException("BTC phải có đơn vị quản lý");
            }
            if (chucVu == null || chucVu.trim().isEmpty()) {
                throw new IllegalArgumentException("BTC phải có chức vụ");
            }
        } else if ("SINHVIEN".equalsIgnoreCase(roleName) || "SINH_VIEN".equalsIgnoreCase(roleName)) {
            if (mssv == null || mssv.trim().isEmpty()) {
                throw new IllegalArgumentException("Sinh viên phải có MSSV");
            }
            if (ngaySinh == null) {
                throw new IllegalArgumentException("Sinh viên phải có ngày sinh");
            }
            int age = LocalDate.now().getYear() - ngaySinh.getYear();
            if (age < 16 || age > 60) {
                throw new IllegalArgumentException("Tuổi sinh viên phải từ 16-60");
            }
            if (gioiTinh == null || gioiTinh.trim().isEmpty()) {
                throw new IllegalArgumentException("Sinh viên phải có giới tính");
            }
            if (donViId == null) {
                throw new IllegalArgumentException("Sinh viên phải thuộc đơn vị/khoa");
            }
            if (tenLop == null || tenLop.trim().isEmpty()) {
                throw new IllegalArgumentException("Sinh viên phải có lớp");
            }
        }
    }
}