package com.example.vadoo.service.btc;

import com.example.vadoo.dto.btc.PointHistoryDTO;
import com.example.vadoo.dto.btc.RegistrationDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportRegistrationsToExcel(List<RegistrationDTO> students, String eventName) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách tham gia");

            // 1. Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"STT", "Họ và Tên", "MSSV", "Lớp", "Thời gian ĐK", "Trạng thái", "Điểm cộng"};

            // Style cho Header (In đậm)
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // 2. Data Rows
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (RegistrationDTO st : students) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(st.getHoTen());
                row.createCell(2).setCellValue(st.getMssv());
                row.createCell(3).setCellValue(st.getLop());
                row.createCell(4).setCellValue(st.getThoiGianDangKy().format(formatter));

                // Translate trạng thái sang tiếng Việt
                String statusVi = switch (st.getTrangThai()) {
                    case "PRESENT" -> "Có mặt";
                    case "ABSENT" -> "Vắng";
                    default -> "Đã đăng ký";
                };
                row.createCell(5).setCellValue(statusVi);

                row.createCell(6).setCellValue(st.getDiem() != null ? st.getDiem() : 0);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel: " + e.getMessage());
        }
    }

    /**
     * Hàm MỚI: Xuất lịch sử điểm rèn luyện
     */
    public ByteArrayInputStream exportPointHistory(List<PointHistoryDTO> history, String semesterName) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Lịch sử điểm");

            // 1. Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"STT", "MSSV", "Họ và Tên", "Lớp", "Sự kiện tham gia", "Thời gian", "Trạng thái", "Điểm"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // 2. Data Rows
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

            for (PointHistoryDTO item : history) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(item.getMssv());
                row.createCell(2).setCellValue(item.getHoTen());
                row.createCell(3).setCellValue(item.getTenLop());
                row.createCell(4).setCellValue(item.getTenSuKien());

                // Thời gian (có thể null nếu sự kiện chưa kết thúc và sv vắng)
                if (item.getThoiGianCheckIn() != null) {
                    row.createCell(5).setCellValue(item.getThoiGianCheckIn().format(formatter));
                } else {
                    row.createCell(5).setCellValue("-");
                }

                // Trạng thái
                String statusVi = "PRESENT".equals(item.getTrangThai()) ? "Có mặt" : "Vắng mặt";
                row.createCell(6).setCellValue(statusVi);

                // Điểm (Số nguyên)
                row.createCell(7).setCellValue(item.getDiem());
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi tạo file Excel: " + e.getMessage());
        }
    }
}