package com.example.vadoo.dto.btc;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class BTCEventCreateDTO {

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Size(min = 5, max = 200, message = "Tên sự kiện phải từ 5 - 200 ký tự")
    private String tieuDe;

    @NotBlank(message = "Địa điểm không được để trống")
    private String diaDiem;

    @NotBlank(message = "Vui lòng nhập mô tả ngắn")
    private String moTa;

    private String posterUrl; // Tạm thời dùng String URL

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    @Future(message = "Thời gian bắt đầu phải ở tương lai")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime thoigianBatdau;

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime thoigianKetthuc;

    @NotNull(message = "Thời gian mở đăng ký là bắt buộc")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime thoigianMoDangky;

    @NotNull(message = "Thời gian đóng đăng ký là bắt buộc")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime thoigianDongDangky;

    @Min(value = 0, message = "Giới hạn không được âm")
    private Integer gioiHan = 0;

    @Min(value = 0, message = "Điểm cộng không được âm")
    private Integer diemCong = 0;

    @Min(value = 0, message = "Điểm tối thiểu không được âm")
    private Integer diemThoiThieu = 0;
}