package com.example.vadoo.service.admin;

import com.example.vadoo.dto.admin.StudentAttendanceDTO;
import com.example.vadoo.dto.admin.StudentPointDTO;
import com.example.vadoo.dto.admin.SystemLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class AdminExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Xuất danh sách sinh viên ra file Excel
     */
    public byte[] exportStudentsToExcel(List<StudentAttendanceDTO> students, String eventName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Danh sách sinh viên");

            // ✅ TẠO STYLE MỘT LẦN DUY NHẤT Ở ĐÂY
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subTitleStyle = createSubTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle attendedStyle = createAttendedStyle(workbook);     // Style đã điểm danh
            CellStyle notAttendedStyle = createNotAttendedStyle(workbook); // Style chưa điểm danh
            CellStyle summaryStyle = createSummaryStyle(workbook);

            // Row 0: Tiêu đề
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH SINH VIÊN ĐĂNG KÝ SỰ KIỆN");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));

            // Row 1: Tên sự kiện
            Row eventRow = sheet.createRow(1);
            Cell eventCell = eventRow.createCell(0);
            eventCell.setCellValue("Sự kiện: " + eventName);
            eventCell.setCellStyle(subTitleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 7));

            // Row 2: Trống
            sheet.createRow(2);

            // Row 3: Header
            Row headerRow = sheet.createRow(3);
            String[] headers = {"STT", "MSSV", "Họ tên", "Lớp", "Email", "SĐT", "Thời gian ĐK", "Điểm danh"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 4;
            int stt = 1;
            for (StudentAttendanceDTO student : students) {
                Row row = sheet.createRow(rowNum++);

                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(stt++);
                cell0.setCellStyle(centerStyle);

                // MSSV
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(student.getMssv() != null ? student.getMssv() : "");
                cell1.setCellStyle(dataStyle);

                // Họ tên
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(student.getTen() != null ? student.getTen() : "");
                cell2.setCellStyle(dataStyle);

                // Lớp
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(student.getTenLop() != null ? student.getTenLop() : "");
                cell3.setCellStyle(dataStyle);

                // Email
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(student.getEmail() != null ? student.getEmail() : "");
                cell4.setCellStyle(dataStyle);

                // SĐT
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(student.getSdt() != null ? student.getSdt() : "");
                cell5.setCellStyle(dataStyle);

                // Thời gian đăng ký
                Cell cell6 = row.createCell(6);
                if (student.getThoiGianDangKy() != null) {
                    cell6.setCellValue(student.getThoiGianDangKy().format(DATE_FORMATTER));
                }
                cell6.setCellStyle(centerStyle);

                // Điểm danh (Sử dụng style đã tạo sẵn)
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(student.getDaDiemDanh() ? "Đã điểm danh" : "Chưa điểm danh");

                // ✅ SỬ DỤNG STYLE TÁI SỬ DỤNG
                if (student.getDaDiemDanh()) {
                    cell7.setCellStyle(attendedStyle);
                } else {
                    cell7.setCellStyle(notAttendedStyle);
                }
            }

            // Tổng kết
            Row summaryRow = sheet.createRow(rowNum + 1);
            Cell summaryCell = summaryRow.createCell(0);
            long attendedCount = students.stream().filter(StudentAttendanceDTO::getDaDiemDanh).count();
            summaryCell.setCellValue(String.format("Tổng: %d sinh viên | Đã điểm danh: %d | Chưa điểm danh: %d",
                    students.size(), attendedCount, students.size() - attendedCount));
            summaryCell.setCellStyle(summaryStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum + 1, rowNum + 1, 0, 7));

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Thêm padding một chút cho đẹp
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportLogsToExcel(List<SystemLogDTO> logs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("System Logs");

            // --- Styles ---
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            // --- Header ---
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Thời gian", "Người thực hiện", "Vai trò", "Hành động", "Đối tượng", "Mô tả", "IP"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data Rows ---
            int rowIdx = 1;
            for (SystemLogDTO log : logs) {
                Row row = sheet.createRow(rowIdx++);

                createCell(row, 0, String.valueOf(log.getId()), centerStyle);
                createCell(row, 1, log.getThoigianTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), centerStyle);
                createCell(row, 2, log.getUserName(), dataStyle);
                createCell(row, 3, log.getUserRole(), centerStyle);
                createCell(row, 4, log.getAction(), centerStyle);
                createCell(row, 5, log.getTargetName(), dataStyle);
                createCell(row, 6, log.getDescription(), dataStyle);
                createCell(row, 7, log.getIpAddress(), centerStyle);
            }

            // Auto size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ========== STYLE HELPERS ==========

    // Helper tạo cell nhanh
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    // ✅ FIX: Đổi tên method
    private CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createAttendedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNotAttendedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSummaryStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    public byte[] exportStudentPointsToExcel(List<StudentPointDTO> students, String reportTitle) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Báo cáo điểm");

            // --- Style Header ---
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // --- Row 0: Title ---
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(reportTitle.toUpperCase());
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            // --- Row 2: Header Columns ---
            Row headerRow = sheet.createRow(2);
            String[] columns = {"STT", "MSSV", "Họ Tên", "Khoa/Lớp", "Số sự kiện", "Điểm Tổng", "Xếp loại"};

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data Rows ---
            int rowIdx = 3;
            int stt = 1;
            for (StudentPointDTO student : students) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(student.getMssv());
                row.createCell(2).setCellValue(student.getTen());
                row.createCell(3).setCellValue(student.getKhoaName());
                row.createCell(4).setCellValue(student.getSoSuKien());
                row.createCell(5).setCellValue(student.getDiemTong());
                row.createCell(6).setCellValue(student.getXepLoai());
            }

            // Auto size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}