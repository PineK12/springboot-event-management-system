package com.example.vadoo.service.admin;

import com.example.vadoo.dto.admin.LogDetailDTO;
import com.example.vadoo.dto.admin.LogFilterDTO;
import com.example.vadoo.dto.admin.SystemLogDTO;
import com.example.vadoo.entity.SystemLog;
import com.example.vadoo.entity.User;
import com.example.vadoo.repository.SystemLogRepository;
import com.example.vadoo.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminLogService {

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ========== GHI LOG ==========

    /**
     * Ghi log hành động của user
     */
    @Transactional
    public void logAction(String username, String action, String targetTable,
                          String targetId, String description,
                          Object oldValue, Object newValue,
                          HttpServletRequest request) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);

            // ✅ FIX: Đổi tên biến để tránh conflict với SLF4J logger
            SystemLog systemLog = SystemLog.builder()
                    .user(user)
                    .action(action)
                    .targetTable(targetTable)
                    .targetId(targetId)
                    .description(description)
                    .oldValue(convertToJson(oldValue))
                    .newValue(convertToJson(newValue))
                    .status("SUCCESS")
                    .ipAddress(getClientIp(request))
                    .build();

            systemLogRepository.save(systemLog);
            log.info("Logged action: {} by {} on {}", action, username, targetTable);

        } catch (Exception e) {
            log.error("Error logging action", e);
        }
    }

    /**
     * Ghi log đơn giản (chỉ description)
     */
    @Transactional
    public void logSimple(String username, String action, String description, HttpServletRequest request) {
        logAction(username, action, null, null, description, null, null, request);
    }

    /**
     * Ghi log login
     */
    @Transactional
    public void logLogin(String username, boolean success, HttpServletRequest request) {
        String action = success ? "LOGIN_SUCCESS" : "LOGIN_FAILED";
        String description = success
                ? "Đăng nhập thành công"
                : "Đăng nhập thất bại";

        logSimple(username, action, description, request);
    }

    /**
     * Ghi log logout
     */
    @Transactional
    public void logLogout(String username, HttpServletRequest request) {
        logSimple(username, "LOGOUT", "Đăng xuất khỏi hệ thống", request);
    }

    // ========== TRA CỨU LOG ==========

    /**
     * Lấy danh sách logs với filter
     */
    public Page<SystemLogDTO> searchLogs(LogFilterDTO filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 20
        );

        LocalDateTime startDate = filter.getStartDate() != null
                ? filter.getStartDate().atStartOfDay()
                : null;

        LocalDateTime endDate = filter.getEndDate() != null
                ? filter.getEndDate().atTime(23, 59, 59)
                : null;

        Page<SystemLog> logs = systemLogRepository.searchLogs(
                filter.getKeyword(),
                mapActionType(filter.getActionType()),
                startDate,
                endDate,
                pageable
        );

        return logs.map(this::convertToDTO);
    }

    /**
     * Lấy chi tiết 1 log
     */
    public LogDetailDTO getLogDetail(Long id) {
        SystemLog systemLog = systemLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));

        return LogDetailDTO.builder()
                .logId(systemLog.getId())
                .action(systemLog.getAction())
                .userName(systemLog.getUser() != null ? systemLog.getUser().getHoTen() : "System")
                .userRole(systemLog.getUser() != null ? systemLog.getUser().getRole().getTenRole() : "SYSTEM")
                .targetName(systemLog.getDescription())
                .time(systemLog.getThoigianTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .description(systemLog.getDescription())
                .oldValueMap(parseJson(systemLog.getOldValue()))
                .newValueMap(parseJson(systemLog.getNewValue()))
                .build();
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert object to JSON string
     */
    private String convertToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            return obj.toString();
        }
    }

    /**
     * Parse JSON string to Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON", e);
            Map<String, Object> map = new HashMap<>();
            map.put("raw", json);
            return map;
        }
    }

    /**
     * Convert entity to DTO
     */
    private SystemLogDTO convertToDTO(SystemLog systemLog) {
        String userName = "System";
        String userRole = "SYSTEM";

        if (systemLog.getUser() != null) {
            userName = systemLog.getUser().getHoTen();
            userRole = systemLog.getUser().getRole().getTenRole();
        }

        return SystemLogDTO.builder()
                .id(systemLog.getId())
                .userName(userName)
                .userRole(userRole)
                .action(systemLog.getAction())
                .targetTable(systemLog.getTargetTable())
                .targetId(systemLog.getTargetId())
                .targetName(extractTargetName(systemLog))
                .description(systemLog.getDescription())
                .status(systemLog.getStatus())
                .ipAddress(systemLog.getIpAddress())
                .thoigianTao(systemLog.getThoigianTao())
                .oldValue(systemLog.getOldValue())
                .newValue(systemLog.getNewValue())
                .build();
    }

    /**
     * Extract friendly target name from log
     */
    private String extractTargetName(SystemLog systemLog) {
        if (systemLog.getDescription() != null) {
            return systemLog.getDescription();
        }

        String table = systemLog.getTargetTable() != null ? systemLog.getTargetTable() : "N/A";
        String id = systemLog.getTargetId() != null ? systemLog.getTargetId() : "";

        return String.format("%s #%s", table, id);
    }

    /**
     * Map action type từ UI sang DB
     */
    private String mapActionType(String uiAction) {
        // 1. Nếu là null hoặc 'all' thì trả về 'all' để Repository bỏ qua điều kiện lọc
        if (uiAction == null || "all".equalsIgnoreCase(uiAction) || uiAction.trim().isEmpty()) {
            return "all";
        }

        // 2. Hỗ trợ legacy (nếu còn link cũ dùng chữ 'auth')
        if ("auth".equalsIgnoreCase(uiAction)) {
            return "LOGIN";
        }

        // 3. Với các trường hợp khác (CREATE, UPDATE, APPROVE...),
        // chỉ cần chuyển thành chữ hoa để khớp với dữ liệu trong DB.
        // Cách này giúp bạn không cần sửa code ở đây khi thêm action mới bên Controller.
        return uiAction.toUpperCase();
    }

    /**
     * Lấy IP address của client
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "0.0.0.0";

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * Thống kê logs
     */
    public Map<String, Long> getLogStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", systemLogRepository.count());
        stats.put("create", systemLogRepository.countByAction("CREATE"));
        stats.put("update", systemLogRepository.countByAction("UPDATE"));
        stats.put("delete", systemLogRepository.countByAction("DELETE"));
        stats.put("auth", systemLogRepository.countByAction("LOGIN_SUCCESS"));
        return stats;
    }

    /**
     * Xóa logs cũ hơn N ngày
     */
    @Transactional
    public void deleteLogsOlderThan(int days) {
        if (days < 10) {
            throw new IllegalArgumentException("Không thể xóa logs mới hơn 30 ngày để đảm bảo an toàn dữ liệu.");
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        systemLogRepository.deleteOldLogs(cutoffDate);

        // Ghi log hành động này (tự mình xóa mình :D)
        // Lưu ý: Log này sẽ được tạo SAU khi xóa, nên nó sẽ tồn tại.
//        logAction("system", "DELETE_LOGS", "system_logs", "BATCH",
//                "Đã xóa logs cũ hơn " + days + " ngày", null, null, null);
    }
}